package ru.last.lastitems.commands;

import dev.by1337.cmd.Command;
import dev.by1337.core.command.bcmd.CommandWrapper;
import dev.by1337.yaml.YamlMap;

import org.bukkit.command.CommandSender;

import ru.last.lastitems.LastItemsFree;

import java.util.List;

public class MainCommand {
    private final LastItemsFree plugin;
    private CommandWrapper wrapper;

    public MainCommand(LastItemsFree plugin) {
        this.plugin = plugin;
        buildAndRegister();
    }

    public void buildAndRegister() {
        if (wrapper != null) {
            wrapper.close();
        }
        Command<CommandSender> root = buildCommandTree();
        wrapper = new CommandWrapper(root, plugin);
        wrapper.setAliases(List.of("litems", "items"));
        wrapper.register();
    }

    public void unregister() {
        if (wrapper != null) {
            wrapper.close();
        }
    }

    private Command<CommandSender> buildCommandTree() {
        Command<CommandSender> main = new Command<>("lastitems");
        YamlMap cmdConfig = plugin.getConfigManager().getCommandsConfig();
        YamlMap folderConfig = plugin.getConfigManager().getFolderConfig();
        ArgumentFactory argFactory = new ArgumentFactory(plugin);

        main.requires(sender -> {
            if (!sender.hasPermission("lastitems.admin")) {
                plugin.getConfigManager().getMessages().getGeneral().getNoPermission().sendToSender(sender);
                return false;
            }
            return true;
        });

        main.executor(this::sendUsage);

        try {
            com.google.common.reflect.ClassPath cp = com.google.common.reflect.ClassPath.from(plugin.getClass().getClassLoader());
            for (com.google.common.reflect.ClassPath.ClassInfo info : cp.getTopLevelClasses("ru.last.lastitems.commands.sub")) {
                Class<?> clazz = info.load();
                if (clazz.isAnnotationPresent(SubCommand.class) && ISubCommand.class.isAssignableFrom(clazz)) {
                    SubCommand meta = clazz.getAnnotation(SubCommand.class);
                    
                    // Check if command is enabled in config (if applicable)
                    if (!meta.cmd().equals("addon") && !meta.cmd().equals("gui") && !meta.cmd().equals("folder")) {
                        if (!isCommandEnabled(meta.cmd(), cmdConfig)) continue;
                    }
                    if (meta.cmd().equals("folder") && !folderConfig.get("enable").asBool(true)) continue;

                    ISubCommand subCommandInstance = (ISubCommand) clazz.getDeclaredConstructor().newInstance();
                    Command<CommandSender> cmdNode = new Command<>(meta.cmd());
                    if (meta.aliases().length > 0) {
                        cmdNode.aliases(meta.aliases());
                    }
                    
                    subCommandInstance.build(cmdNode, plugin, argFactory);
                    main.sub(cmdNode);
                }
            }
        } catch (Exception e) {
            plugin.getDebugLogger().error("Ошибка при регистрации подкоманд", e);
        }

        return main;
    }

    private boolean isCommandEnabled(String name, YamlMap config) {
        return config.get("commands." + name + ".enable").asBool(true);
    }

    private void sendUsage(CommandSender sender) {
        plugin.getConfigManager().getMessages().getGeneral().getUsage().send(sender, null,
                "%command%", "lastitems",
                "%args%", "<addon/folder/give/giveall/take/takeall/list/reload(rl)>");
    }
}