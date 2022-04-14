package com.github.mrmks.mc.cscriptjava;

import com.github.mrmks.mc.cscriptjava.engine.SharedClassPool;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.io.File;

@Mod(modid = "CnpcScriptJava", version = "0.0.1", acceptableRemoteVersions = "*")
public class CnpcScriptJava {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SharedClassPool.init(new File(event.getModConfigurationDirectory(), "CnpcScriptJava/build"));
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {
            @Override
            public String getName() {
                return "csj";
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return "csj [sub]";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (args.length > 1) {
                    String sub = args[0];
                    if (sub.equalsIgnoreCase("del")) {
                        String msg = SharedClassPool.remove(args[1]) ? "success" : "Not exist";
                        sender.sendMessage(new TextComponentString(msg));
                    } else if (sub.equalsIgnoreCase("test")) {
                        String msg = SharedClassPool.contain(args[1]) ? "Exist" : "Not exist";
                        sender.sendMessage(new TextComponentString(msg));
                    }
                }
            }
        });
    }
}
