package ink.magma.luckcmd;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LuckCmd extends JavaPlugin implements TabExecutor {
    public static JavaPlugin instance;
    ConfigurationSection cmdGroup;
    boolean enablePlaceholder = false;
    BukkitScheduler scheduler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        saveDefaultConfig();
        cmdGroup = LuckCmd.instance.getConfig().getConfigurationSection("cmd-groups");
        scheduler = Bukkit.getScheduler();

        if (Bukkit.getPluginCommand("luckcmd") != null) {
            Bukkit.getPluginCommand("luckcmd").setExecutor(this);
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            enablePlaceholder = true;
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 有一个参数
        if (args.length >= 1) {
            if (args[0].equals("run") || args[0].equals("runscript")) {
                if (!sender.hasPermission("zth.luckcmd.run")) {
                    sender.sendMessage("您没有此命令的权限.");
                    return true;
                }
                if (args.length == 1) {
                    sender.sendMessage("请提供指令包名!");
                    return true;
                }

                if (args.length == 2 || args.length == 3) {
                    // 拿指令列表
                    List<String> list = LuckCmd.instance.getConfig().getStringList("cmd-groups." + args[1]);
                    if (list.isEmpty()) {
                        sender.sendMessage("此随机指令预设不存在 / 为空!");
                        return true;
                    }

                    // 开始执行指令, 寻找一个合适的变量主人
                    CommandSender papiOwner = null;
                    if (sender instanceof Player) {
                        papiOwner = sender;
                    }
                    if (sender instanceof ConsoleCommandSender && args.length == 3) {
                        papiOwner = Bukkit.getPlayer(args[2]);
                    }

                    // 如果是随机模式, 抽指令
                    if (args[0].equals("run")) {
                        int luckIndex = (int) (Math.random() * list.size());
                        String luckCommand = list.get(luckIndex);

                        runACommand(luckCommand, papiOwner);
                    }
                    // 如果是顺序执行模式(脚本模式), 遍历列表
                    if (args[0].equals("runscript")) {
                        CommandSender finalPapiOwner = papiOwner;
                        list.forEach(listCommand -> runACommand(listCommand, finalPapiOwner));
                    }

                }
                return true;
            }

            if (args[0].equals("list")) {
                if (!sender.hasPermission("zth.luckcmd.admin")) {
                    sender.sendMessage("您没有此命令的权限.");
                    return true;
                }
                if (args.length == 1) {
                    sender.sendMessage("以下是现有的随机指令组:");
                    for (Map.Entry<String, Object> group : cmdGroup.getValues(false).entrySet()) {
                        sender.sendMessage(group.getKey());
                    }
                }
                if (args.length == 2) {
                    if (cmdGroup.get(args[1]) == null) {
                        sender.sendMessage("此指令包不存在!");
                        return true;
                    }
                    List<String> thisGroup = cmdGroup.getStringList(args[1]);
                    sender.sendMessage("指令组 " + args[1] + " 包含以下命令: (索引和指令可点击)");
                    for (int i = 0; i < thisGroup.size(); i++) {
                        Component btn = MiniMessage.miniMessage()
                                .deserialize("[" + i + "] ")
                                .clickEvent(ClickEvent.suggestCommand("/luckcmd edit " + args[1] + " remove " + i));
                        Component cmd = MiniMessage.miniMessage()
                                .deserialize(thisGroup.get(i))
                                .clickEvent(ClickEvent.suggestCommand(thisGroup.get(i)));
                        sender.sendMessage(btn.append(cmd));
                    }
                    if (thisGroup.size() == 0) {
                        sender.sendMessage("(什么也没有)");
                    }
                }
                return true;
            }

            if (args[0].equals("create")) {
                if (!sender.hasPermission("zth.luckcmd.admin")) {
                    sender.sendMessage("您没有此命令的权限.");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage("请提供一个指令包名");
                    return true;
                }
                if (cmdGroup.get(args[1]) != null) {
                    sender.sendMessage("此指令包已经存在!");
                    return true;
                }
                cmdGroup.set(args[1], new ArrayList<>());
                saveConfig();
                sender.sendMessage("已创建.");
                return true;
            }

            if (args[0].equals("remove")) {
                if (!sender.hasPermission("zth.luckcmd.admin")) {
                    sender.sendMessage("您没有此命令的权限.");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage("请提供一个指令包名");
                    return true;
                }
                if (cmdGroup.get(args[1]) == null) {
                    sender.sendMessage("此指令包名不存在, 因此无法删除");
                    return true;
                }
                cmdGroup.set(args[1], null);
                saveConfig();
                sender.sendMessage("已经删除指定的指令包.");
                return true;
            }

            if (args[0].equals("edit")) {
                if (!sender.hasPermission("zth.luckcmd.admin")) {
                    sender.sendMessage("您没有此命令的权限.");
                    return true;
                }
                if (args.length == 1) {
                    sender.sendMessage("请提供要编辑的指令包名");
                    return true;
                }
                if (args.length >= 2) {
                    if (args.length <= 3) {
                        sender.sendMessage("缺失参数");
                        return true;
                    }
                    if (cmdGroup.get(args[1]) == null) {
                        sender.sendMessage("此指令包不存在, 请先创建一个");
                        return true;
                    }
                    List<String> oldList = cmdGroup.getStringList(args[1]);
                    if (args[2].equals("add")) {
                        String addedCommand = "";
                        for (int i = 0; i < args.length; i++) {
                            if (i >= 3) {
                                addedCommand = addedCommand + args[i] + " ";
                            }
                        }
                        oldList.add(addedCommand.trim());
                        cmdGroup.set(args[1], oldList);
                        sender.sendMessage("指令: \"" + addedCommand.trim() + "\"已添加.");
                    }
                    if (args[2].equals("remove")) {
                        int removeIndex;
                        try {
                            removeIndex = Integer.parseInt(args[3]);
                            if (oldList.size() <= removeIndex || removeIndex < 0) {
                                sender.sendMessage("您提供的数字范围错误");
                                return true;
                            }
                        } catch (Exception e) {
                            sender.sendMessage("请您提供要删除指令的索引 ID, 为整数");
                            return true;
                        }

                        String removedCommand = oldList.get(removeIndex);
                        oldList.remove(removeIndex);
                        cmdGroup.set(args[1], oldList);
                        sender.sendMessage("指令: \"" + removedCommand + "\"已删除.");
                        Bukkit.dispatchCommand(sender, "luckcmd list " + args[1]);
                    }
                    saveConfig();
                    return true;
                }
            }

            if (args[0].equals("help")) {
                if (!sender.hasPermission("zth.luckcmd.admin")) {
                    sender.sendMessage("您没有此命令的权限.");
                    return true;
                }
                sender.sendMessage("""
                        \n
                        LuckCmd Help - MagmaBlock
                                               
                        /luckcmd run <group> [player] - 进行一次随机 (若在后台执行, 则必须跟上 player 参数)
                        /luckcmd runscript <group> [player] - 顺序执行一个列表 (若在后台执行, 则必须跟上 player 参数)
                        /luckcmd list - 查看所有指令包列表
                        /luckcmd list <group> - 查看某个指令包
                        /luckcmd create <group> - 创建一个指令包
                        /luckcmd remove <group> - 删除一个指令包
                        /luckcmd edit <group> add [@delay <ticks>][@for <次数> [延迟 ticks]]<command> - 指令包添加新指令
                        /luckcmd edit <group> remove <index> - 使用索引删除一个指令
                        \n
                        """);
                return true;
            }
        }
        return false;
    }

    final List<String> mainParams = List.of("run", "runscript", "list", "create", "remove", "edit", "help");
    final List<String> editParams = List.of("add", "remove");

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String
            label, @NotNull String[] args) {
        if (args.length == 1) {
            return mainParams;
        }
        if (args.length == 3 && args[0].equals("edit")) {
            return editParams;
        }
        return null;
    }

    /**
     * 执行一个指令
     *
     * @param command 指令，从配置文件中取出的 String
     * @param sender  可以是 Null, 用于解析变量主人
     */
    void runACommand(@NotNull String command, @Nullable CommandSender sender) {
        // 解析指令
        String commandParsed = command;
        // 解析 Placeholder 变量
        if (enablePlaceholder) { // 如果服务器支持 变量
            if (sender != null) { // 如果发送者是玩家, 变量以玩家身份解析
                commandParsed = PlaceholderAPI.setPlaceholders((Player) sender, commandParsed);
            }
        }
        // 解析是否含有自定义指令标签
        ExecutableCommand parseResult = new ExecutableCommand(commandParsed);

        // 执行指令
        if (parseResult.runTimes != 1) {
            // 遍历执行次数次, 每次都按照延迟挂起一个延迟任务
            for (int t = 0; t < parseResult.runTimes; t++) {
                scheduler.scheduleSyncDelayedTask(this, () -> {
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), parseResult.realCommand);
                }, parseResult.delayTicks + (long) t * parseResult.forDelayTicks);
            }
        } else {
            scheduler.scheduleSyncDelayedTask(this, () -> {
                Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), parseResult.realCommand);
            }, parseResult.delayTicks);
        }
    }
}


