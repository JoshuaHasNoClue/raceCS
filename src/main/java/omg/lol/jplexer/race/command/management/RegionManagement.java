package omg.lol.jplexer.race.command.management;

import com.j256.ormlite.dao.Dao;
import net.md_5.bungee.api.ChatColor;
import omg.lol.jplexer.race.Race;
import omg.lol.jplexer.race.command.RaceCompleter;
import omg.lol.jplexer.race.models.Region;
import omg.lol.jplexer.race.models.Station;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static omg.lol.jplexer.race.Race.CHAT_PREFIX;

public class RegionManagement {
    static class PunchHandler implements Listener {
        interface PunchHandlerEventHandler {
            void onPlayerInteract(PlayerInteractEvent event);
        }

        PunchHandlerEventHandler handler;

        static void OneShot(Player player, PunchHandlerEventHandler eventHandler) {
            PunchHandler handler = new PunchHandler();
            Race.getPlugin().getServer().getPluginManager().registerEvents(handler, Race.getPlugin());

            handler.handler = (event) -> {
                if (event.getPlayer() == player) {

                    if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        HandlerList.unregisterAll(handler);
                        eventHandler.onPlayerInteract(event);
                    }
                }
            };
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            event.setCancelled(true);

            handler.onPlayerInteract(event);
        }
    }

    public static void RegionManagementCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("racecs.manage")) {
            sender.sendMessage(CHAT_PREFIX + ChatColor.RED + "Sorry, you can't use this.");
            return;
        }

        String verb = args.length == 0 ? "help" : args[0];
        switch (verb) {
            case "help":
                sender.sendMessage("For information on defining station regions, " +
                        "see the \"RaceCS\" section on the AirCS Specifications Handbook: https://bit.ly/aircs_spec");
                break;
            case "add":
                AddRegion(sender, args[1]);
                break;
            case "query":
                QueryRegion(sender);
                break;
            case "remove":
                RemoveRegion(sender);
                break;
            case "track":
                TogglePlayerTracking(sender);
                break;
        }
    }

    public static List<String> TabCompleteCommand(String[] args) {
        if (args.length == 0) {
            return Arrays.asList("help", "add", "query", "remove", "track");
        } else {
            Dao<Station, String> stationDao = Race.getPlugin().getStationDao();

            ArrayList<String> stations = new ArrayList<>();
            stationDao.forEach(station -> stations.add(station.getId()));

            switch (args[0]) {
                case "help":
                case "query":
                case "remove":
                    return null;
                case "add":
                    return RaceCompleter.completeList(Arrays.copyOfRange(args, 1, args.length), stations.toArray(new String[0]));
                default:
                    return Arrays.asList("help", "add", "query", "remove", "track");
            }
        }
    }

    public static void AddRegion(CommandSender sender, String station) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Sorry, only a player can add regions.");
            return;
        }

        Dao<Station, String> stationDao = Race.getPlugin().getStationDao();
        Dao<Region, Long> regionDao = Race.getPlugin().getRegionDao();
        try {
            Station stationObject = stationDao.queryForId(station.toUpperCase());
            if (stationObject == null) throw new Exception();

            sender.sendMessage("Left click on a corner of the region. Right click to cancel.");
            PunchHandler.OneShot((Player) sender, firstCorner -> {
                if (firstCorner.getAction() == Action.RIGHT_CLICK_AIR || firstCorner.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    sender.sendMessage("Cancelled adding a region.");
                    return;
                }

                sender.sendMessage("Left click on the other corner of the region. Right click to cancel.");
                PunchHandler.OneShot((Player) sender, secondCorner -> {
                    if (secondCorner.getAction() == Action.RIGHT_CLICK_AIR || secondCorner.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        sender.sendMessage("Cancelled adding a region.");
                        return;
                    }

                    if (firstCorner.getClickedBlock().getWorld() != secondCorner.getClickedBlock().getWorld()) {
                        sender.sendMessage("The corners must be in the same world. Aborting.");
                        return;
                    }

                    Region region = new Region();
                    region.setStation(stationObject);

                    region.setX1(firstCorner.getClickedBlock().getX());
                    region.setY1(firstCorner.getClickedBlock().getY());
                    region.setZ1(firstCorner.getClickedBlock().getZ());

                    region.setX2(secondCorner.getClickedBlock().getX());
                    region.setY2(secondCorner.getClickedBlock().getY());
                    region.setZ2(secondCorner.getClickedBlock().getZ());

                    region.setWorld(secondCorner.getClickedBlock().getWorld().getName());

                    try {
                        regionDao.create(region);
                        sender.sendMessage("A new region was added to " + stationObject.getHumanReadableName());
                    } catch (SQLException e) {
                        sender.sendMessage("Could not create the station region.");
                    }
                });
            });
        } catch (Exception e) {
            sender.sendMessage("Could not find that station.");
        }
    }

    public static void QueryRegion(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a player can use this command.");
            return;
        }

        Player player = (Player) sender;
        Dao<Region, Long> regionDao = Race.getPlugin().getRegionDao();
        ArrayList<Station> stations = new ArrayList<>();
        regionDao.forEach(region -> {
            if (region.inRegion(player.getLocation())) stations.add(region.getStation());
        });

        if (stations.isEmpty()) {
            sender.sendMessage("You are in no defined regions.");
        } else {
            sender.sendMessage("You are in the following regions:");
            for (Station station : stations) sender.sendMessage(station.getHumanReadableName());
        }
    }

    public static void RemoveRegion(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a player can use this command.");
            return;
        }

        Player player = (Player) sender;
        Dao<Region, Long> regionDao = Race.getPlugin().getRegionDao();
        ArrayList<Region> regions = new ArrayList<>();
        regionDao.forEach(region -> {
            if (region.inRegion(player.getLocation())) regions.add(region);
        });

        if (regions.isEmpty()) {
            sender.sendMessage("You are in no defined regions.");
        } else {
            for (Region region : regions) {
                try {
                    regionDao.delete(region);
                    sender.sendMessage("Removed a region for " + region.getStation().getHumanReadableName());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void TogglePlayerTracking(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a player can use this command.");
            return;
        }

        Race.getPlugin().getStationTracker().TogglePlayerTracking((Player) sender);
    }
}
