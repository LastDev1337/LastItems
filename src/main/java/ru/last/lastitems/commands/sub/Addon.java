package ru.last.lastitems.commands.sub;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import org.bukkit.command.CommandSender;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.addons.AddonManager;
import ru.last.lastitems.addons.AbstractAddon;
import ru.last.lastitems.commands.ArgumentFactory;
import ru.last.lastitems.commands.ISubCommand;
import ru.last.lastitems.commands.SubCommand;
import ru.last.lastitems.commands.ArgSuggest;

import java.util.List;

@SubCommand(cmd = "addon")
public class Addon implements ISubCommand {

    @Override
    public void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory) {
        command.requires(sender -> sender.hasPermission("lastitems.admin"));
        
        command.argument(new ArgSuggest("action", () -> List.of("load", "unload", "reload", "list")));
        command.argument(new ArgumentString<>("addonName"));

        command.executor((sender, args) -> {
            String action = (String) args.getOrDefault("action", "");
            String addonName = (String) args.get("addonName");
            AddonManager am = ru.last.lastitems.api.LastItemsAPI.getInstance().getAddonManager();

            if (am == null) {
                sender.sendMessage("AddonManager недоступен.");
                return;
            }

            switch (action.toLowerCase()) {
                case "load":
                    sender.sendMessage("Динамическая загрузка пока не поддерживается, используйте перезагрузку плагина.");
                    break;
                case "unload":
                    sender.sendMessage("Динамическая выгрузка пока не поддерживается, используйте перезагрузку плагина.");
                    break;
                case "reload":
                    am.disableAddons();
                    am.loadAddons();
                    am.enableAddons();
                    sender.sendMessage("§a[LastItems] Аддоны успешно перезагружены!");
                    break;
                case "list":
                    List<AbstractAddon> addons = am.getLoadedAddons();
                    if (addons.isEmpty()) {
                        sender.sendMessage("§c[LastItems] Нет загруженных аддонов.");
                    } else {
                        sender.sendMessage("§a[LastItems] Загруженные аддоны (" + addons.size() + "):");
                        for (AbstractAddon addon : addons) {
                            sender.sendMessage("§7- §f" + addon.getName() + " §ev" + addon.getVersion() + " §7от §b" + addon.getAuthor());
                        }
                    }
                    break;
                default:
                    sender.sendMessage("Использование: /lastitems addon <load/unload/reload/list> [name]");
                    break;
            }
        });
    }
}
