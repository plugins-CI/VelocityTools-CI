/*
 * Copyright (C) 2021 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.velocitytools;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.elytrium.velocitytools.commands.AlertCommand;
import net.elytrium.velocitytools.commands.FindCommand;
import net.elytrium.velocitytools.commands.HubCommand;
import net.elytrium.velocitytools.commands.SendCommand;
import net.elytrium.velocitytools.commands.VelocityToolsCommand;
import net.elytrium.velocitytools.hooks.PluginMessageHook;
import net.elytrium.velocitytools.listeners.BrandChangerListener;
import net.elytrium.velocitytools.listeners.HostnamesManagerListener;
import net.elytrium.velocitytools.listeners.ProtocolBlockerJoinListener;
import net.elytrium.velocitytools.listeners.ProtocolBlockerPingListener;
import org.slf4j.Logger;

@Plugin(
    id = "velocity_tools",
    name = "Velocity Tools",
    version = BuildConstants.VERSION,
    url = "https://elytrium.net",
    authors = {"mdxd44", "hevav"}
)
public class VelocityTools {

  private static VelocityTools instance;

  private final ProxyServer server;
  private final Path dataDirectory;
  private final Logger logger;

  private Toml config;

  @Inject
  public VelocityTools(ProxyServer server, @DataDirectory Path dataDirectory, Logger logger) {
    instance = this;
    this.logger = logger;
    this.server = server;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    this.reload();

    PluginMessageHook.init();
  }

  public static VelocityTools getInstance() {
    return instance;
  }

  public void reload() {
    try {
      if (!this.dataDirectory.toFile().exists()) {
        //noinspection ResultOfMethodCallIgnored
        this.dataDirectory.toFile().mkdir();
      }

      File configFile = new File(this.dataDirectory.toFile(), "config.toml");
      if (!configFile.exists()) {
        Files.copy(Objects.requireNonNull(VelocityTools.class.getResourceAsStream("/config.toml")),
            configFile.toPath()
        );
      }
      this.config = new Toml().read(new File(this.dataDirectory.toFile(), "config.toml"));
    } catch (IOException e) {
      this.logger.error("Unable to load configuration!", e);
    }

    // Commands /////////////////////////
    if (this.config.getBoolean("commands.hub.enabled") && !this.config.getList("commands.hub.aliases").isEmpty()) {
      List<String> aliases = this.config.getList("commands.hub.aliases");
      this.server.getCommandManager().unregister(aliases.get(0));
      this.server.getCommandManager().register(
          aliases.get(0),
          new HubCommand(this, this.server),
          aliases.toArray(new String[0])
      );
    }

    if (this.config.getBoolean("commands.alert.enabled")) {
      this.server.getCommandManager().unregister("alert");
      this.server.getCommandManager().register("alert", new AlertCommand(this, this.server));
    }

    if (this.config.getBoolean("commands.find.enabled")) {
      this.server.getCommandManager().unregister("find");
      this.server.getCommandManager().register("find", new FindCommand(this, this.server));
    }

    if (this.config.getBoolean("commands.send.enabled")) {
      this.server.getCommandManager().unregister("send");
      this.server.getCommandManager().register("send", new SendCommand(this, this.server));
    }

    this.server.getCommandManager().unregister("velocitytools");
    this.server.getCommandManager().register("velocitytools", new VelocityToolsCommand(this), "vtools");
    ///////////////////////////////////

    // Tools /////////////////////////
    this.server.getEventManager().unregisterListeners(this);

    if (this.config.getBoolean("tools.brandchanger.enabled")) {
      this.server.getEventManager().register(this, new BrandChangerListener(this));
    }

    if (this.config.getBoolean("tools.protocolblocker.block_ping")) {
      this.server.getEventManager().register(this, new ProtocolBlockerPingListener(this));
    }

    if (this.config.getBoolean("tools.protocolblocker.block_joining")) {
      this.server.getEventManager().register(this, new ProtocolBlockerJoinListener(this));
    }

    if (this.config.getBoolean("tools.hostnamesmanager.enabled")) {
      this.server.getEventManager().register(this, new HostnamesManagerListener(this));
    }
    ///////////////////////////////////
  }

  public Toml getConfig() {
    return this.config;
  }

  public ProxyServer getServer() {
    return server;
  }
}
