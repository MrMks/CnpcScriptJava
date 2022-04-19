package com.github.mrmks.mc.cscriptjava;

import com.github.mrmks.mc.cscriptjava.engine.SharedClassPool;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mod(modid = "cnpcscriptjava", name = "CnpcScriptJava", version = "0.0.1", acceptableRemoteVersions = "*")
public class CnpcScriptJava {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SharedClassPool.init(new File(event.getModConfigurationDirectory(), "CnpcScriptJava/build"));
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        PermissionAPI.registerNode("csj.cmd", DefaultPermissionLevel.OP, "Permission to use csj commands");
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
            public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
                if (!(sender instanceof EntityPlayer) || PermissionAPI.hasPermission((EntityPlayer) sender, "csj.cmd")) {
                    if (args.length == 0 || args[0].isEmpty()) {
                        return Arrays.asList("test", "del");
                    } else if (args.length == 1) {
                        return getListOfStringsMatchingLastWord(args, "test", "del");
                    }
                }
                return Collections.emptyList();
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (!(sender instanceof EntityPlayer) || PermissionAPI.hasPermission(((EntityPlayer) sender), "csj.cmd")) {
                    if (args.length < 1 || args[0].isEmpty()) {
                        sender.sendMessage(new TextComponentString("test, del"));
                    } else {
                        String sub = args[0];
                        if (sub.equalsIgnoreCase("del")) {
                            if (args.length < 2 || args[1].isEmpty()) sender.sendMessage(new TextComponentString("You should input a class name"));
                            else {
                                String cn = args[1];
                                String msg = SharedClassPool.remove(args[1]) ? "Success to remove class \"" + cn + "\"" : "Class \"" + cn + "\" doesn't exist or failed to delete.";
                                sender.sendMessage(new TextComponentString(msg));
                            }
                        } else if (sub.equalsIgnoreCase("test")) {
                            if (args.length < 2 || args[1].isEmpty()) sender.sendMessage(new TextComponentString("You should input a class name"));
                            else {
                                String cn = args[1];
                                String msg = "Class \"" + cn + "\" " + (SharedClassPool.contain(cn) ? "exist." : "doesn't exist.");
                                sender.sendMessage(new TextComponentString(msg));
                            }
                        } else {
                            sender.sendMessage(new TextComponentString("test, del"));
                        }
                    }
                }
            }
        });
    }
}
