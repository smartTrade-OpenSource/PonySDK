/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

// The terminal's scripts (docs/decisions/0004-terminal-chunks.md), in build/dist:
// - terminal.js, the core, an ES module; it loads each module on demand from chunks/<name>.js, and exports what they
//   use of it (src/core-api.ts);
// - chunks/<name>.js, one per module loaded on demand, importing the core from terminal.js: they share no code of
//   their own, so terminal.js imports nothing before the page is built;
// - legacy.js, the legacy layer, a classic script.
import { build } from 'esbuild';
import { readFileSync } from 'node:fs';
import { basename, resolve } from 'node:path';

const OUT = 'build/dist';
const ON_DEMAND = ['date-picker', 'rich-text', 'layout', 'stack-layout-panel', 'tree', 'menu', 'suggest'];
/** Modules a chunk keeps for itself: no other module uses them. */
const OWNED = new Set([resolve('src/runtime/date-format.ts')]);
const common = { bundle: true, target: 'es2022', minify: true, sourcemap: true, logLevel: 'warning', metafile: true };
const onDemand = new Set(ON_DEMAND.map(name => resolve(`src/objects/${name}.ts`)));

/** In the core, an import of a module loaded on demand goes to its chunk. */
const toChunks = {
    name: 'to-chunks',
    setup(b) {
        b.onResolve({ filter: /^\.\.\/objects\// }, args => {
            const path = resolve(args.resolveDir, args.path);
            if (!onDemand.has(path)) return undefined;
            if (args.kind !== 'dynamic-import') throw new Error(`${args.importer} imports ${args.path} statically`);
            return { path: `./chunks/${basename(path, '.ts')}.js`, external: true };
        });
    },
};

/** In a chunk, what is not the chunk's own comes from the core. */
const fromCore = {
    name: 'from-core',
    setup(b) {
        b.onResolve({ filter: /.*/ }, args => {
            if (args.kind === 'entry-point') return undefined;
            const path = args.path === '#model' ? '#model' : resolve(args.resolveDir, args.path);
            if (onDemand.has(path) && args.importer === path) return undefined;
            if (OWNED.has(path)) return undefined;
            return { path: '../terminal.js', external: true };
        });
    },
};

const core = await build({ ...common, entryPoints: { terminal: 'src/main.ts' }, format: 'esm', outdir: OUT, plugins: [toChunks] });
const chunks = await build({
    ...common, format: 'esm', outdir: OUT, plugins: [fromCore],
    entryPoints: Object.fromEntries(ON_DEMAND.map(name => [`chunks/${name}`, `src/objects/${name}.ts`])),
});
await build({ ...common, entryPoints: { legacy: 'src/legacy.ts' }, format: 'iife', outdir: OUT });

// A chunk importing a name the core does not export would fail when loaded: fail here instead
const exported = new Set(core.metafile.outputs[`${OUT}/terminal.js`].exports);
const missing = [];
for (const file of Object.keys(chunks.metafile.outputs).filter(f => f.endsWith('.js'))) {
    for (const match of readFileSync(file, 'utf8').matchAll(/import\{([^}]*)\}from"\.\.\/terminal\.js"/g)) {
        for (const binding of match[1].split(',')) {
            const name = binding.split(' as ')[0].trim();
            if (!exported.has(name)) missing.push(`${basename(file)}: ${name}`);
        }
    }
}
if (missing.length > 0) throw new Error(`Names the chunks import and the core does not export (src/core-api.ts): ${missing.join(', ')}`);
