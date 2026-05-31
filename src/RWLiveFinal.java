import java.lang.instrument.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.List;

public class RWLiveFinal {
    private static final String JSON_FILE = "d:/tiexiuzhanz/rw_units.json";
    private static volatile boolean running = true;

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        new Thread("RWLiveFinal") {
            public void run() {
                try {
                    Thread.sleep(5000);
                    initAndLoop();
                } catch (Exception e) {
                    logError(e);
                }
            }
        }.start();
    }

    private static void initAndLoop() {
        try {
            // ---- Unit reflection setup ----
            Class<?> amClass = Class.forName("com.corrodinggames.rts.game.units.am");
            Field beField = amClass.getDeclaredField("bE");
            beField.setAccessible(true);
            Object list = beField.get(null);

            Class<?> uClass = Class.forName("com.corrodinggames.rts.gameFramework.utility.u");
            Method sizeMethod = uClass.getDeclaredMethod("size"); sizeMethod.setAccessible(true);
            Method getMethod = uClass.getDeclaredMethod("a", int.class); getMethod.setAccessible(true);

            Field dnField = amClass.getDeclaredField("dn"); dnField.setAccessible(true);
            Field dmField = amClass.getDeclaredField("dm"); dmField.setAccessible(true);
            Field cuField = amClass.getDeclaredField("cu"); cuField.setAccessible(true);
            Field cvField = amClass.getDeclaredField("cv"); cvField.setAccessible(true);

            Class<?> wClass = Class.forName("com.corrodinggames.rts.gameFramework.w");
            Field eoField = wClass.getDeclaredField("eo"); eoField.setAccessible(true);
            Field epField = wClass.getDeclaredField("ep"); epField.setAccessible(true);

            Method rMethod = amClass.getDeclaredMethod("r");
            rMethod.setAccessible(true);

            // ---- Team reflection setup ----
            Class<?> nClass = Class.forName("com.corrodinggames.rts.game.n");
            Field teamsField = nClass.getDeclaredField("b");
            teamsField.setAccessible(true);

            Field teamIdField = nClass.getDeclaredField("k"); teamIdField.setAccessible(true);
            Field creditsField = nClass.getDeclaredField("o"); creditsField.setAccessible(true);
            Field energyField = nClass.getDeclaredField("p"); energyField.setAccessible(true);
            Field unitCountField = nClass.getDeclaredField("q"); unitCountField.setAccessible(true);
            Field colorIdField = nClass.getDeclaredField("r"); colorIdField.setAccessible(true);
            Field nameField = nClass.getDeclaredField("v"); nameField.setAccessible(true);
            Field aiLevelField = nClass.getDeclaredField("Q"); aiLevelField.setAccessible(true);
            Field aiBaseLevelField = nClass.getDeclaredField("R"); aiBaseLevelField.setAccessible(true);
            Field isAIField = nClass.getDeclaredField("n"); isAIField.setAccessible(true);
            Field isDefeatedField = nClass.getDeclaredField("u"); isDefeatedField.setAccessible(true);
            Field isActiveField = nClass.getDeclaredField("m"); isActiveField.setAccessible(true);
            Field commandCenterField = nClass.getDeclaredField("s"); commandCenterField.setAccessible(true);

            // ---- Map reflection setup ----
            Class<?> engineClass = Class.forName("com.corrodinggames.rts.gameFramework.l");
            Method getEngineMethod = engineClass.getDeclaredMethod("B");
            getEngineMethod.setAccessible(true);
            Object engine = getEngineMethod.invoke(null);

            Field tileMapField = engineClass.getDeclaredField("bL"); tileMapField.setAccessible(true);
            Object tileMap = tileMapField.get(engine);

            Class<?> tileMapClass = Class.forName("com.corrodinggames.rts.game.b.b");
            Field tileCountXField = tileMapClass.getDeclaredField("C"); tileCountXField.setAccessible(true);
            Field tileCountYField = tileMapClass.getDeclaredField("D"); tileCountYField.setAccessible(true);
            Field tileSizeXField = tileMapClass.getDeclaredField("n"); tileSizeXField.setAccessible(true);
            Field tileSizeYField = tileMapClass.getDeclaredField("o"); tileSizeYField.setAccessible(true);
            Field groundLayerField = tileMapClass.getDeclaredField("u"); groundLayerField.setAccessible(true);
            Field unitObjectsField = tileMapClass.getDeclaredField("A"); unitObjectsField.setAccessible(true);
            Field uniqueTilesField = tileMapClass.getDeclaredField("B"); uniqueTilesField.setAccessible(true);

            Class<?> mapLayerClass = Class.forName("com.corrodinggames.rts.game.b.e");
            Field layerWField = mapLayerClass.getDeclaredField("n"); layerWField.setAccessible(true);
            Field layerHField = mapLayerClass.getDeclaredField("o"); layerHField.setAccessible(true);
            Field tileIdsField = mapLayerClass.getDeclaredField("q"); tileIdsField.setAccessible(true);

            Class<?> mapTileClass = Class.forName("com.corrodinggames.rts.game.b.g");
            Field isWaterField = mapTileClass.getDeclaredField("e"); isWaterField.setAccessible(true);
            Field isCliffField = mapTileClass.getDeclaredField("h"); isCliffField.setAccessible(true);
            Field isResourceField = mapTileClass.getDeclaredField("i"); isResourceField.setAccessible(true);
            Field blockLevelField = mapTileClass.getDeclaredField("j"); blockLevelField.setAccessible(true);
            Field hasLargeObjField = mapTileClass.getDeclaredField("k"); hasLargeObjField.setAccessible(true);

            while (running) {
                export(list, sizeMethod, getMethod,
                       dnField, dmField, cuField, cvField, eoField, epField, rMethod,
                       teamsField, teamIdField, creditsField, energyField, unitCountField,
                       colorIdField, nameField, aiLevelField, aiBaseLevelField,
                       isAIField, isDefeatedField, isActiveField, commandCenterField,
                       tileMap, tileCountXField, tileCountYField, tileSizeXField, tileSizeYField,
                       groundLayerField, unitObjectsField, uniqueTilesField,
                       layerWField, layerHField, tileIdsField,
                       isWaterField, isCliffField, isResourceField, blockLevelField, hasLargeObjField);
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            logError(e);
        }
    }

    private static void export(Object list, Method sizeMethod, Method getMethod,
                               Field dnField, Field dmField, Field cuField, Field cvField,
                               Field eoField, Field epField, Method rMethod,
                               Field teamsField, Field teamIdField, Field creditsField,
                               Field energyField, Field unitCountField, Field colorIdField,
                               Field nameField, Field aiLevelField, Field aiBaseLevelField,
                               Field isAIField, Field isDefeatedField, Field isActiveField,
                               Field commandCenterField,
                               Object tileMap, Field tileCountXField, Field tileCountYField,
                               Field tileSizeXField, Field tileSizeYField,
                               Field groundLayerField, Field unitObjectsField, Field uniqueTilesField,
                               Field layerWField, Field layerHField, Field tileIdsField,
                               Field isWaterField, Field isCliffField, Field isResourceField,
                               Field blockLevelField, Field hasLargeObjField) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");

        // ---- Map metadata + resources + terrain ----
        int mapW = 0, mapH = 0, tileSize = 20;
        try {
            if (tileMap != null) {
                mapW = tileCountXField.getInt(tileMap);
                mapH = tileCountYField.getInt(tileMap);
                tileSize = tileSizeXField.getInt(tileMap);
                int worldW = mapW * tileSize;
                int worldH = mapH * tileSize;
                json.append(String.format("  \"map\":{\"widthTiles\":%d,\"heightTiles\":%d,\"tileSize\":%d,\"worldWidth\":%d,\"worldHeight\":%d},\n",
                    mapW, mapH, tileSize, worldW, worldH));
            } else {
                json.append("  \"map\":null,\n");
            }
        } catch (Exception e) {
            json.append("  \"map\":{\"error\":\"").append(safeStr(e.getMessage())).append("\"},\n");
        }

        // Resources
        json.append("  \"resources\": [\n");
        try {
            if (tileMap != null) {
                Object unitObjects = unitObjectsField.get(tileMap);
                if (unitObjects instanceof List) {
                    List<?> pts = (List<?>) unitObjects;
                    int rc = 0;
                    for (Object p : pts) {
                        if (p == null) continue;
                        int px = p.getClass().getField("x").getInt(p);
                        int py = p.getClass().getField("y").getInt(p);
                        if (rc > 0) json.append(",\n");
                        json.append(String.format("    {\"x\":%d,\"y\":%d}", px, py));
                        rc++;
                    }
                }
            }
        } catch (Exception e) {
            json.append("    {\"error\":\"").append(safeStr(e.getMessage())).append("\"}");
        }
        json.append("\n  ],\n");

        // Terrain mask
        json.append("  \"terrain\": ");
        try {
            if (tileMap != null) {
                Object groundLayer = groundLayerField.get(tileMap);
                Object uniqueTiles = uniqueTilesField.get(tileMap);
                if (groundLayer != null && uniqueTiles instanceof Object[]) {
                    int layerW = layerWField.getInt(groundLayer);
                    int layerH = layerHField.getInt(groundLayer);
                    short[] tileIds = (short[]) tileIdsField.get(groundLayer);
                    Object[] tileLookup = (Object[]) uniqueTiles;

                    // Downsample to ~50x50 max
                    int target = 50;
                    int stepX = Math.max(1, layerW / target);
                    int stepY = Math.max(1, layerH / target);
                    int outW = layerW / stepX;
                    int outH = layerH / stepY;

                    StringBuilder mask = new StringBuilder();
                    mask.append(String.format("{\"width\":%d,\"height\":%d,\"step\":%d,\"mask\":[", outW, outH, Math.max(stepX, stepY)));
                    for (int oy = 0; oy < outH; oy++) {
                        if (oy > 0) mask.append(",");
                        mask.append("\"");
                        for (int ox = 0; ox < outW; ox++) {
                            int sx = ox * stepX;
                            int sy = oy * stepY;
                            int ex = Math.min(sx + stepX, layerW);
                            int ey = Math.min(sy + stepY, layerH);

                            char ch = '.'; // default land
                            // Priority: cliff > water > resource > obstacle > land
                            outer:
                            for (int ty = sy; ty < ey; ty++) {
                                for (int tx = sx; tx < ex; tx++) {
                                    int idx = tx * layerH + ty;
                                    if (idx < 0 || idx >= tileIds.length) continue;
                                    short tid = tileIds[idx];
                                    if (tid < 0 || tid >= tileLookup.length) continue;
                                    Object mt = tileLookup[tid];
                                    if (mt == null) continue;

                                    if (isCliffField.getBoolean(mt)) { ch = '^'; break outer; }
                                    if (isWaterField.getBoolean(mt)) { ch = '~'; }
                                    else if (isResourceField.getBoolean(mt) && ch == '.') { ch = '*'; }
                                    else if ((hasLargeObjField.getBoolean(mt) || blockLevelField.getByte(mt) > 0) && ch == '.') { ch = '@'; }
                                }
                            }
                            mask.append(ch);
                        }
                        mask.append("\"");
                    }
                    mask.append("]}");
                    json.append(mask.toString());
                } else {
                    json.append("null");
                }
            } else {
                json.append("null");
            }
        } catch (Exception e) {
            json.append("{\"error\":\"").append(safeStr(e.getMessage())).append("\"}");
        }
        json.append(",\n");

        // ---- Teams ----
        json.append("  \"teams\": [\n");
        try {
            Object[] teams = (Object[]) teamsField.get(null);
            int teamCount = 0;
            if (teams != null) {
                for (Object team : teams) {
                    if (team == null) continue;
                    if (!isActiveField.getBoolean(team)) continue;

                    int tid = teamIdField.getInt(team);
                    double credits = creditsField.getDouble(team);
                    double energy = energyField.getDouble(team);
                    int ucount = unitCountField.getInt(team);
                    int colorId = colorIdField.getInt(team);
                    String tname = (String) nameField.get(team);
                    int aiLvl = aiLevelField.getInt(team);
                    int aiBase = aiBaseLevelField.getInt(team);
                    boolean isAI = isAIField.getBoolean(team);
                    boolean isDefeated = isDefeatedField.getBoolean(team);

                    double baseX = -1, baseY = -1;
                    Object cc = commandCenterField.get(team);
                    if (cc != null) {
                        try {
                            baseX = eoField.getFloat(cc);
                            baseY = epField.getFloat(cc);
                        } catch (Exception ignored) {}
                    }

                    if (teamCount > 0) json.append(",\n");
                    json.append(String.format(
                        "    {\"teamId\":%d,\"name\":\"%s\",\"credits\":%.1f,\"energy\":%.1f,\"unitCount\":%d,\"colorId\":%d,\"aiLevel\":%d,\"aiBaseLevel\":%d,\"isAI\":%s,\"isDefeated\":%s,\"baseX\":%.1f,\"baseY\":%.1f}",
                        tid, safeStr(tname), credits, energy, ucount, colorId, aiLvl, aiBase,
                        isAI, isDefeated, baseX, baseY));
                    teamCount++;
                }
            }
        } catch (Exception e) {
            json.append("    {\"error\":\"").append(safeStr(e.getMessage())).append("\"}");
        }
        json.append("\n  ],\n");

        // ---- Units ----
        json.append("  \"units\": [\n");
        try {
            int size = (int) sizeMethod.invoke(list);
            int count = 0;
            for (int i = 0; i < size; i++) {
                Object unit = getMethod.invoke(list, i);
                if (unit == null) continue;
                int team = dnField.getInt(unit);
                if (team < 0) continue;

                int typeId = dmField.getInt(unit);
                float hp = cuField.getFloat(unit);
                float maxHp = cvField.getFloat(unit);
                float x = eoField.getFloat(unit);
                float y = epField.getFloat(unit);
                String typeName = unit.getClass().getSimpleName();

                String enumName = "";
                String displayName = "";
                try {
                    Object def = rMethod.invoke(unit);
                    if (def != null) {
                        Method nameMethod = def.getClass().getMethod("name");
                        enumName = (String) nameMethod.invoke(def);
                        Method eMethod = def.getClass().getDeclaredMethod("e");
                        eMethod.setAccessible(true);
                        displayName = (String) eMethod.invoke(def);
                    }
                } catch (Exception ex) {}
                if (enumName == null) enumName = "";
                if (displayName == null) displayName = "";

                enumName = safeStr(enumName);
                displayName = safeStr(displayName);

                if (count > 0) json.append(",\n");
                json.append(String.format(
                    "    {\"i\":%d,\"team\":%d,\"type\":\"%s\",\"typeId\":%d,\"enum\":\"%s\",\"name\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"hp\":%.1f,\"maxHp\":%.1f}",
                    i, team, typeName, typeId, enumName, displayName, x, y, hp, maxHp));
                count++;
            }
        } catch (Exception e) {
            json.append("    {\"error\":\"").append(safeStr(e.getMessage())).append("\"}");
        }
        json.append("\n  ]\n}\n");

        try (FileWriter fw = new FileWriter(JSON_FILE, false)) { fw.write(json.toString()); }
        catch (IOException e) {}
    }

    private static String safeStr(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private static void logError(Exception e) {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("d:/tiexiuzhanz/agent_err.txt", true));
            e.printStackTrace(pw);
            pw.close();
        } catch (IOException ie) {}
    }
}
