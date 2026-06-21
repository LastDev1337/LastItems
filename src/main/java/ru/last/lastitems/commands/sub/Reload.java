package ru.last.lastitems.commands.sub;

import dev.by1337.cmd.Command;
import org.bukkit.command.CommandSender;
import ru.last.lastitems.LastItemsFree;
import ru.last.lastitems.commands.ArgumentFactory;
import ru.last.lastitems.commands.ISubCommand;
import ru.last.lastitems.commands.SubCommand;

@SubCommand(cmd = "reload", aliases = {"rl"})
public class Reload implements ISubCommand {

    @Override
    public void build(Command<CommandSender> command, LastItemsFree plugin, ArgumentFactory argFactory) {
        command.requires(sender -> sender.hasPermission(plugin.getConfigManager().getCommandsConfig().get("commands.reload.permission").asString("lastitems.admin")));
        
        command.executor(sender -> {
            long start = System.currentTimeMillis();
            try {
                plugin.getConfigManager().loadAll();
                plugin.getItemLoader().loadItems();
                
                var addonManager = ru.last.lastitems.api.LastItemsAPI.getInstance().getAddonManager();
                if (addonManager != null) {
                    addonManager.disableAddons();
                    addonManager.loadAddons();
                    addonManager.enableAddons();
                }
                
                // Note: since wrapper is re-registered in MainCommand, we should call buildAndRegister
                // We'll expose it in MainCommand or just use plugin.getMainCommand().buildAndRegister();
                if (plugin.getMainCommand() != null) {
                    plugin.getMainCommand().buildAndRegister();
                }
                
                long time = System.currentTimeMillis() - start;

                plugin.getConfigManager().getMessages().getGeneral().getReloadSuccess().send(sender, null, "%time%", String.valueOf(time));
            } catch (Exception e) {
                plugin.getConfigManager().getMessages().getGeneral().getReloadError().sendToSender(sender);
                plugin.getDebugLogger().error("Reload error", e);
            }
        });
    }
}
