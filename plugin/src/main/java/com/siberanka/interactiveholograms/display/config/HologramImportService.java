package com.siberanka.interactiveholograms.display.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/** Single bounded entry point for every supported modern-schema import. */
public final class HologramImportService {
    private final FancyHologramsImporter fancy;
    private final DecentHologramsImporter decent;
    private final LegacyPluginHologramImporter legacy;
    private final AtomicBoolean importing = new AtomicBoolean();

    public HologramImportService(Path serverRoot, Path dataFolder) {
        this.fancy = new FancyHologramsImporter(serverRoot, dataFolder);
        this.decent = new DecentHologramsImporter(serverRoot, dataFolder);
        this.legacy = new LegacyPluginHologramImporter(serverRoot, dataFolder);
    }

    public ImportResult importFrom(HologramImportSource source, String path, boolean overwrite) throws IOException {
        if (source == null) throw new IOException("Unknown hologram source.");
        if (!importing.compareAndSet(false, true)) throw new IOException("Another hologram import is already running.");
        try {
            switch (source) {
                case FANCY_HOLOGRAMS:
                    FancyHologramsImporter.ImportResult fancyResult = fancy.importYaml(path, overwrite);
                    return new ImportResult(fancyResult.getImported(), fancyResult.getSkipped(), 0, fancyResult.getSource());
                case DECENT_HOLOGRAMS:
                    DecentHologramsImporter.ImportResult decentResult = decent.importYaml(path, overwrite);
                    return new ImportResult(decentResult.getImported(), decentResult.getSkipped(), decentResult.getWarnings(), decentResult.getSource());
                default:
                    return legacy.importYaml(source, path, overwrite);
            }
        } finally {
            importing.set(false);
        }
    }

    public static final class ImportResult {
        private final int imported, skipped, warnings; private final Path source;
        ImportResult(int imported, int skipped, int warnings, Path source) {
            this.imported = imported; this.skipped = skipped; this.warnings = warnings; this.source = source;
        }
        public int getImported() { return imported; }
        public int getSkipped() { return skipped; }
        public int getWarnings() { return warnings; }
        public Path getSource() { return source; }
    }
}
