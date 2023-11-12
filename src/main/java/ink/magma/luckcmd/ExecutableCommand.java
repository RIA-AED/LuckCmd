package ink.magma.luckcmd;

public class ExecutableCommand {
    public String realCommand;
    public int delayTicks;
    public int runTimes;
    public int forDelayTicks;

    ExecutableCommand(String command) {
        // 默认值
        realCommand = command;
        delayTicks = 0;
        runTimes = 1;
        forDelayTicks = 0;

        // 解析 [@delay <tick>]
        int delayIndex = command.indexOf("[@delay ");
        if (delayIndex != -1) {
            int endIndex = command.indexOf("]", delayIndex);
            if (endIndex != -1) {
                String delayString = command.substring(delayIndex + 7, endIndex).trim();
                try {
                    delayTicks = Integer.parseInt(delayString);
                } catch (NumberFormatException e) {
                    // 处理解析错误
                }
                realCommand = command.substring(endIndex + 1).trim();
            }
        }

        // 解析 [@for <次数> [间隔]]
        int forIndex = command.indexOf("[@for ");
        if (forIndex != -1) {
            int endIndex = command.indexOf("]", forIndex);
            if (endIndex != -1) {
                String forString = command.substring(forIndex + 5, endIndex).trim();
                String[] forArray = forString.split(" ");
                try {
                    runTimes = Integer.parseInt(forArray[0]);
                    if (forArray.length > 1) {
                        forDelayTicks = Integer.parseInt(forArray[1]);
                    }
                } catch (NumberFormatException e) {
                    // 处理解析错误
                }
                realCommand = command.substring(endIndex + 1).trim();
            }
        }
    }

}
