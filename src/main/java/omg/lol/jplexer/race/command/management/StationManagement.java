package omg.lol.jplexer.race.command.management;

import com.j256.ormlite.dao.Dao;
import net.md_5.bungee.api.ChatColor;
import omg.lol.jplexer.race.Race;
import omg.lol.jplexer.race.command.RaceCompleter;
import omg.lol.jplexer.race.models.Station;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static omg.lol.jplexer.race.Race.CHAT_PREFIX;

public class StationManagement {
    public static void StationManagementCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("racecs.manage")) {
            sender.sendMessage(CHAT_PREFIX + ChatColor.RED + "Sorry, you can't use this.");
            return;
        }

        String verb = args.length == 0 ? "help" : args[0];
        switch (verb) {
            case "help":
                sender.sendMessage("For information on adding stations, see the \"RaceCS\" section in the AirCS " +
                        "Specifications Handbook: https://bit.ly/aircs_spec");
                break;
            case "add":
                AddStation(sender, args[1], args[2]);
                break;
            case "remove":
                RemoveStation(sender, args[1]);
                break;
            case "rename":
                RenameStation(sender, args[1], args[2]);
                break;
            case "list":
                ListStations(sender);
                break;
        }
    }

    public static List<String> TabCompleteCommand(String[] args) {
        if (args.length == 0) {
            return Arrays.asList("help", "add", "remove", "list", "rename");
        } else {
            Dao<Station, String> stationDao = Race.getPlugin().getStationDao();

            ArrayList<String> stations = new ArrayList<>();
            stationDao.forEach(station -> stations.add(station.getId()));

            switch (args[0]) {
                case "help":
                case "list":
                    return null;
                case "remove":
                    return RaceCompleter.completeList(Arrays.copyOfRange(args, 1, args.length), stations.toArray(new String[0]));
                case "add":
                    return Collections.singletonList("<stationId> <stationName>");
                case "rename":
                    if (args.length >= 2 && stations.contains(args[1])) {
                        return Collections.singletonList("<newName>");
                    } else {
                        return RaceCompleter.completeList(Arrays.copyOfRange(args, 1, args.length), stations.toArray(new String[0]));
                    }
                default:
                    return Arrays.asList("help", "add", "remove", "list", "rename");
            }
        }
    }

    public static void AddStation(CommandSender sender, String id, String name) {
        Dao<Station, String> stationDao = Race.getPlugin().getStationDao();

        Station station = new Station();
        station.setId(id);
        station.setName(name);

        try {
            stationDao.create(station);
            sender.sendMessage("Created the station " + station.getHumanReadableName());
        } catch (SQLException e) {
            sender.sendMessage("Could not create the station");
        }
    }

    public static void RenameStation(CommandSender sender, String id, String name) {
        Dao<Station, String> stationDao = Race.getPlugin().getStationDao();

        try {
            Station station = stationDao.queryForId(id.toUpperCase());
            if (station == null) {
                sender.sendMessage("That is not a valid station");
                return;
            }

            station.setName(name);
            stationDao.update(station);
            sender.sendMessage("Renamed the station " + station.getHumanReadableName());
        } catch (SQLException e) {
            sender.sendMessage("Could not rename the station");
        }
    }

    public static void RemoveStation(CommandSender sender, String id) {
        Dao<Station, String> stationDao = Race.getPlugin().getStationDao();
        try {
            Station station = stationDao.queryForId(id.toUpperCase());
            if (station == null) {
                sender.sendMessage("That is not a valid station");
                return;
            }

            stationDao.delete(station);
            sender.sendMessage("Deleted the station " + station.getHumanReadableName());
        } catch (SQLException e) {
            sender.sendMessage("Could not remove the station");
        }
    }

    public static void ListStations(CommandSender sender) {
        Dao<Station, String> stationDao = Race.getPlugin().getStationDao();
        sender.sendMessage("Available stations:");
        stationDao.forEach(station -> sender.sendMessage(station.getHumanReadableName()));
    }
}
