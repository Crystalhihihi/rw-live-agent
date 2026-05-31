import java.lang.instrument.*;
import java.lang.reflect.*;
import java.io.*;

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
            Field teamsField = nClass.getDeclaredField("b"); // teamInstances
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

            while (running) {
                export(list, sizeMethod, getMethod,
                       dnField, dmField, cuField, cvField, eoField, epField, rMethod,
                       teamsField, teamIdField, creditsField, energyField, unitCountField,
                       colorIdField, nameField, aiLevelField, aiBaseLevelField,
                       isAIField, isDefeatedField, isActiveField, commandCenterField);
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
                               Field commandCenterField) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");

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

                    // Base position from command center
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
