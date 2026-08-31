package com.chesst.stockfish;

import com.chesst.config.AppProperties;
import com.chesst.stockfish.dto.EngineResult;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side Stockfish integration over the UCI protocol.
 * Spawns one Stockfish process per analysis (bounded by max-concurrent semaphore).
 */
@Service
public class StockfishService {

    private static final Logger log = LoggerFactory.getLogger(StockfishService.class);
    private static final Pattern SCORE_CP = Pattern.compile("score cp (-?\\d+)");
    private static final Pattern SCORE_MATE = Pattern.compile("score mate (-?\\d+)");
    private static final Pattern PV = Pattern.compile(" pv (.+)$");
    private static final Pattern BESTMOVE = Pattern.compile("bestmove (\\S+)");

    private final AppProperties.Stockfish props;
    private final Semaphore slots;
    private final ProcessBuilder builder;

    public StockfishService(AppProperties appProperties) {
        this.props = appProperties.stockfish();
        this.slots = new Semaphore(props.maxConcurrent(), true);
        this.builder = new ProcessBuilder(props.binaryPath()).redirectErrorStream(true);
        log.info("StockfishService initialized: binary={}, depth={}, movetime={}ms, slots={}",
                props.binaryPath(), props.depth(), props.movetimeMs(), props.maxConcurrent());
    }

    public EngineResult analyze(String fen, Integer requestedDepth, Integer movetimeMs) {
        int depth = requestedDepth != null ? Math.min(20, requestedDepth) : props.depth();
        int mt = movetimeMs != null ? movetimeMs : props.movetimeMs();
        try {
            slots.acquire();
            try {
                return runAnalysis(fen, depth, mt);
            } finally {
                slots.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return EngineResult.error(fen, "interrupted");
        } catch (Exception e) {
            log.error("Stockfish analysis failed for fen={}: {}", fen, e.getMessage());
            return EngineResult.error(fen, e.getMessage());
        }
    }

    private EngineResult runAnalysis(String fen, int depth, int movetimeMs) throws IOException, InterruptedException, TimeoutException {
        Process process = builder.start();
        try (OutputStreamWriter in = new OutputStreamWriter(process.getOutputStream());
             BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            send(in, "uci");
            send(in, "setoption name Threads value 2");
            send(in, "setoption name Hash value 64");
            send(in, "isready");
            waitLine(out, "readyok", 3000);
            send(in, "ucinewgame");
            send(in, "position fen " + fen);
            send(in, "go depth " + depth + " movetime " + movetimeMs);

            long deadline = System.currentTimeMillis() + movetimeMs + 2000;
            int lastCp = 0;
            int lastMate = 0;
            int lastDepth = 0;
            int lastNps = 0;
            String lastPv = "";
            String bestMoveUci = null;
            String line;
            while ((line = out.readLine()) != null) {
                if (System.currentTimeMillis() > deadline) break;
                if (line.startsWith("info") && line.contains(" pv ")) {
                    Matcher mCp = SCORE_CP.matcher(line);
                    Matcher mMate = SCORE_MATE.matcher(line);
                    if (mCp.find()) {
                        lastCp = Integer.parseInt(mCp.group(1));
                        lastMate = 0;
                    } else if (mMate.find()) {
                        lastMate = Integer.parseInt(mMate.group(1));
                        lastCp = lastMate > 0 ? 100000 - lastMate * 100 : -100000 - lastMate * 100;
                    }
                    Matcher mDepth = Pattern.compile(" depth (\\d+)").matcher(line);
                    if (mDepth.find()) lastDepth = Integer.parseInt(mDepth.group(1));
                    Matcher mNps = Pattern.compile(" nps (\\d+)").matcher(line);
                    if (mNps.find()) lastNps = Integer.parseInt(mNps.group(1));
                    Matcher mPv = PV.matcher(line);
                    if (mPv.find()) lastPv = mPv.group(1).trim();
                }
                if (line.startsWith("bestmove")) {
                    Matcher m = BESTMOVE.matcher(line);
                    if (m.find()) bestMoveUci = m.group(1);
                    break;
                }
            }
            send(in, "quit");
            return new EngineResult(
                    fen,
                    bestMoveUci != null ? bestMoveUci : "(none)",
                    lastCp,
                    lastMate == 0 ? null : lastMate,
                    lastDepth,
                    lastNps,
                    lastPv,
                    true,
                    null
            );
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private void send(OutputStreamWriter in, String cmd) throws IOException {
        in.write(cmd + "\n");
        in.flush();
    }

    private void waitLine(BufferedReader out, String token, long timeoutMs) throws IOException, TimeoutException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String line;
        while ((line = out.readLine()) != null) {
            if (line.contains(token)) return;
            if (System.currentTimeMillis() > deadline) throw new TimeoutException("Stockfish did not respond: " + token);
        }
    }

    @PreDestroy
    void shutdown() {
        log.info("StockfishService shutting down");
    }
}
