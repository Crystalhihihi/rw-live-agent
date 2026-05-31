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
            Field pathingLayerField = tileMapClass.getDeclaredField("y"); pathingLayerField.setAccessible(true);
            Field unitObjectsField = tileMapClass.getDeclaredField("A"); unitObjectsField.setAccessible(true);
            Field uniqueTilesField = tileMapClass.getDeclaredField("B"); uniqueTilesField.setAccessible(true);

            Class<?> mapLayerClass = Class.forName("com.corrodinggames.rts.game.b.e");
            Field layerWField = mapLayerClass.getDeclaredField("n"); layerWField.setAccessible(true);
            Field layerHField = mapLayerClass.getDeclaredField("o"); layerHField.setAccessible(true);
            Method getTileIdsMethod = mapLayerClass.getDeclaredMethod("a");
            getTileIdsMethod.setAccessible(true);

            Class<?> pathEngineClass = Class.forName("com.corrodinggames.rts.gameFramework.k.l");
            Field pathEngineField = engineClass.getDeclaredField("bU"); pathEngineField.setAccessible(true);

            Class<?> pathCostMapClass = Class.forName("com.corrodinggames.rts.gameFramework.k.i");
            Field landCostMapField = pathEngineClass.getDeclaredField("y"); landCostMapField.setAccessible(true);
            Field costMapWField = pathCostMapClass.getDeclaredField("b"); costMapWField.setAccessible(true);
            Field costMapHField = pathCostMapClass.getDeclaredField("c"); costMapHField.setAccessible(true);
            Field terrainCostField = pathCostMapClass.getDeclaredField("d"); terrainCostField.setAccessible(true);

            while (running) {
                export(list, sizeMethod, getMethod,
                       dnField, dmField, cuField, cvField, eoField, epField, rMethod,
                       teamsField, teamIdField, creditsField, energyField, unitCountField,
                       colorIdField, nameField, aiLevelField, aiBaseLevelField,
                       isAIField, isDefeatedField, isActiveField, commandCenterField,
                       tileMap, tileCountXField, tileCountYField, tileSizeXField, tileSizeYField,
                       groundLayerField, pathingLayerField, unitObjectsField, uniqueTilesField,
                       layerWField, layerHField, getTileIdsMethod,
                       engine, pathEngineField, landCostMapField, costMapWField, costMapHField, terrainCostField);
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
                               Field groundLayerField, Field pathingLayerField, Field unitObjectsField, Field uniqueTilesField,
                               Field layerWField, Field layerHField, Method getTileIdsMethod,
                               Object engine, Field pathEngineField, Field landCostMapField, Field costMapWField, Field costMapHField, Field terrainCostField) {
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
                        // PC obfuscation: Point.x -> a, Point.y -> b
                        int px = p.getClass().getField("a").getInt(p);
                        int py = p.getClass().getField("b").getInt(p);
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

        // Terrain mask from pathfinding grid (actual passability data)
        json.append("  \"terrain\": ");
        try {
            Object pathEngine = pathEngineField.get(engine);
            if (pathEngine != null) {
                Object landCostMap = landCostMapField.get(pathEngine);
                if (landCostMap != null) {
                    int costW = costMapWField.getInt(landCostMap);
                    int costH = costMapHField.getInt(landCostMap);
                    byte[] terrainCost = (byte[]) terrainCostField.get(landCostMap);

                    int target = 50;
                    int stepX = Math.max(1, costW / target);
                    int stepY = Math.max(1, costH / target);
                    int outW = costW / stepX;
                    int outH = costH / stepY;

                    StringBuilder mask = new StringBuilder();
                    mask.append(String.format("{\"width\":%d,\"height\":%d,\"step\":%d,\"mask\":[", outW, outH, Math.max(stepX, stepY)));
                    for (int oy = 0; oy < outH; oy++) {
                        if (oy > 0) mask.append(",");
                        mask.append("\"");
                        for (int ox = 0; ox < outW; ox++) {
                            int sx = ox * stepX;
                            int sy = oy * stepY;
                            int ex = Math.min(sx + stepX, costW);
                            int ey = Math.min(sy + stepY, costH);

                            char ch = '.'; // passable
                            outer:
                            for (int ty = sy; ty < ey; ty++) {
                                for (int tx = sx; tx < ex; tx++) {
                                    int idx = tx * costH + ty;
                                    if (terrainCost == null || idx < 0 || idx >= terrainCost.length) continue;
                                    byte cost = terrainCost[idx];
                                    if (cost == -1) { ch = 'X'; break outer; } // impassable
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

        // ---- Units ----
        int[] teamUnitCounts = new int[16];
        StringBuilder unitsJson = new StringBuilder();
        unitsJson.append("  \"units\": [\n");
        try {
            int size = (int) sizeMethod.invoke(list);
            int count = 0;
            for (int i = 0; i < size; i++) {
                Object unit = getMethod.invoke(list, i);
                if (unit == null) continue;
                int team = dnField.getInt(unit);
                if (team < 0) continue;
                if (team < teamUnitCounts.length) teamUnitCounts[team]++;

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

                if (count > 0) unitsJson.append(",\n");
                unitsJson.append(String.format(
                    "    {\"i\":%d,\"team\":%d,\"type\":\"%s\",\"typeId\":%d,\"enum\":\"%s\",\"name\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"hp\":%.1f,\"maxHp\":%.1f}",
                    i, team, typeName, typeId, enumName, displayName, x, y, hp, maxHp));
                count++;
            }
        } catch (Exception e) {
            unitsJson.append("    {\"error\":\"").append(safeStr(e.getMessage())).append("\"}");
        }
        unitsJson.append("\n  ]\n}\n");

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
                    int ucount = (tid >= 0 && tid < teamUnitCounts.length) ? teamUnitCounts[tid] : 0;
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

        json.append(unitsJson);

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
