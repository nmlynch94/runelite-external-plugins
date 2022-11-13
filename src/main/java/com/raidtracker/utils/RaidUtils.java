package com.raidtracker.utils;


import com.google.inject.Inject;
import com.raidtracker.data.RaidTracker;
import com.raidtracker.RaidTrackerConfig;
import com.raidtracker.RaidTrackerPlugin;
import com.raidtracker.data.UniqueDrop;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static net.runelite.client.util.Text.toJagexName;

public class RaidUtils
{
    @Inject
    private ItemManager itemManager;
    @Inject
    private Client client;

    @Inject
    private RaidTrackerPlugin raidTrackerPlugin;

    @Inject
    private ConfigManager configManager;

    @Inject
    private RaidTrackerConfig config;

    public void parseRaidTime(String message, RaidTracker raidTracker, RaidStateTracker tracker)
    {
        switch (tracker.getCurrentState().getRaidType())
        {
            case 0 :
                String timeString = message.split("complete! Duration: ")[1];

                String[] coxRooms = {
                        "Upper", "Middle", "Lower", "shamans", "vasa","vanguards","mystics","tekton", "muttadiles", "vespula", "ice demon", "thieving", "tightrope", "crabs"
                };

                for (String room : coxRooms)
                {
                    if (message.startsWith(room))
                    {
                        Array.set(raidTracker.roomTimes, ArrayUtils.indexOf(coxRooms, room), raidTrackerPlugin.stringTimeToSeconds(timeString.split(" ")[timeString.split(" ").length - 1]));
                        return;
                    }
                    if (message.toLowerCase().contains(room))
                    {
                        Array.set(raidTracker.roomTimes, ArrayUtils.indexOf(coxRooms, room), raidTrackerPlugin.stringTimeToSeconds(timeString.split(" ")[0]));
                        return;
                    }
                }
                break;
            case 1 :
                if (message.toLowerCase().contains("wave '")) {
                    String wave = message.toLowerCase().split("'")[1];

                    String[] waves = {"the maiden of sugadinti", "the pestilent bloat", "the nylocas", "sotetseg", "xarpus", "the final challenge"};
                    Array.set(
                            raidTracker.roomTimes,
                            ArrayUtils.indexOf(waves, wave),
                            (raidTrackerPlugin.stringTimeToSeconds(message.toLowerCase().split("duration: ")[1].split((wave.equalsIgnoreCase("the final challenge")) ? "theatre" : "total")[0]
                            )));
                }

                if (message.toLowerCase().contains("theatre of blood wave completion")) {
                    raidTracker.setRaidTime(raidTrackerPlugin.stringTimeToSeconds(message.toLowerCase().split("time: ")[1].split("personal")[0]));
                }

                break;
            case 2 :
                String[] toaRooms = {
                        "Path of Crondis", "Zebak", "Path of Apmeken", "Ba-Ba", "Path of Het", "Akkha", "Path of Scabaras", "Kephri", "The Wardens"
                };
                String t = message.split("Duration: ")[1];
                for (String room : toaRooms)
                {
                    if (message.toLowerCase().contains(room.toLowerCase()))
                    {

                        if (message.contains("Tombs"))
                        {
                            String m1 = (message.split("Tombs")[0]).split("Duration: ")[1]; // Warden completion time.
                            Array.set(raidTracker.getRoomTimes(), ArrayUtils.indexOf(toaRooms, room), raidTrackerPlugin.stringTimeToSeconds(m1.split(" ")[0]));
                            String m2 = (message.split("Tombs")[1]).split("time: ")[1].split(". Personal best:")[0]; // Total Completion time.
                            raidTracker.setRaidTime(raidTrackerPlugin.stringTimeToSeconds(m2.split(" ")[0]));
                        } else
                        {
                            String[] bossRooms = {"Zebak", "Ba-Ba", "Akkha", "Kephri"};
                            Array.set(raidTracker.getRoomTimes(), ArrayUtils.indexOf(toaRooms, room), raidTrackerPlugin.stringTimeToSeconds(t.split(" ")[0]));
                            if (ArrayUtils.indexOf(toaRooms, room) %2 != 0)
                            {
                                int pathTime = (int) Array.get(raidTracker.getRoomTimes(), ArrayUtils.indexOf(toaRooms, room) - 1);
                                Array.set(
                                        raidTracker.getRoomTimes(),
                                        toaRooms.length + ArrayUtils.indexOf(bossRooms, room),
                                        (pathTime + raidTrackerPlugin.stringTimeToSeconds(t.split(" ")[0]))
                                );
                            }
                        }
                        return;
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + tracker.getCurrentState().getRaidType());
        }
    }
    
    public void parseRaidUniques(String message, RaidTracker raidTracker, RaidStateTracker tracker)
    {
        String playername = toJagexName(client.getLocalPlayer().getName());
        ArrayList<UniqueDrop> Uniques = raidTracker.getUniques();
        switch (tracker.getCurrentState().getRaidType())
        {
            case 0 :
            {
                String name = message.split(" - ")[0];
                if (name == playername)
                {
                    name =   raidTrackerPlugin.getProfileKey(configManager);
                }
                String drop = message.split(" - ")[1];
                int value = itemManager.search(drop).get(0).getPrice();
                int lootSplit = value / raidTracker.getTeamSize();
                int cutoff = config.FFACutoff();

                Uniques.add(new UniqueDrop(name,drop,value,(config.defaultFFA() || lootSplit < cutoff), raidTracker.getTeamSize()));
                raidTrackerPlugin.getFw().writeToFile(raidTracker);
                break;
            }
            case 1 :
            case 2 : {
                String name = message.split(" found something special: ")[0];
                if (name == playername)
                {
                    name =   raidTrackerPlugin.getProfileKey(configManager);
                }
                String drop = message.split(" found something special: ")[1];
                int value = itemManager.search(drop).get(0).getPrice();
                int lootSplit = value / raidTracker.getTeamSize();
                int cutoff = config.FFACutoff();

                Uniques.add(new UniqueDrop(name,drop,value,(config.defaultFFA() || lootSplit < cutoff), raidTracker.getTeamSize()));
                raidTrackerPlugin.getFw().writeToFile(raidTracker);
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + tracker.getCurrentState().getRaidType());
        }
        raidTracker.setUniques(Uniques);
    }
    
    public void parseUntradables(String message, RaidTracker raidTracker, RaidStateTracker tracker)
    {
        String playername = toJagexName(client.getLocalPlayer().getName());
        ArrayList<UniqueDrop> nTradables = raidTracker.getNTradables();
        switch (tracker.getCurrentState().getRaidType())
        {
            case 0 :
                if (message.startsWith(raidTrackerPlugin.TWISTED_KIT_RECIPIENTS) || message.startsWith(raidTrackerPlugin.DUST_RECIPIENTS))
                {
                    boolean isKit = message.startsWith(raidTrackerPlugin.TWISTED_KIT_RECIPIENTS);
                    String[] recipients = isKit ?
                            message.split(raidTrackerPlugin.TWISTED_KIT_RECIPIENTS)[1].split(",") :
                            message.split(raidTrackerPlugin.DUST_RECIPIENTS)[1].split(",");
                    for (String recipient : recipients)
                    {
                        if (recipient == playername)
                        {
                            recipient =   raidTrackerPlugin.getProfileKey(configManager);
                        }
                        nTradables.add(new UniqueDrop(recipient, isKit ? "Twisted Kit" : "Metamorphic Dust"));
                    }
                    raidTrackerPlugin.getFw().writeToFile(raidTracker);
                }
                break;
            case 1 :
                String name = message.split(" found something special: ")[0];
                if (name == playername)
                {
                    name =   raidTrackerPlugin.getProfileKey(configManager);
                }
                String drop = message.split(" found something special: ")[1];
                nTradables.add(new UniqueDrop(name, drop));
                raidTrackerPlugin.getFw().writeToFile(raidTracker);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + tracker.getCurrentState().getRaidType());
        }
    }
    public void parsePets(String message, RaidTracker raidTracker, RaidStateTracker tracker)
    {
        boolean inOwnName = false;
        boolean duplicate = message.toLowerCase().contains("would have been followed");
        String tmpName = message.split(" ")[0];
        String drop = "";
        String name = duplicate ? raidTrackerPlugin.profileKey : message.split(" ")[0];
        switch (tracker.getCurrentState().getRaidType())
        {
            case 0 : drop = "Olmlet";break;
            case 1 : drop = "Lil' Zik";break;
            case 2 : drop = "Tumeken's guardian";break;
        }
        ArrayList<UniqueDrop> pets = raidTracker.getPets();
        pets.add(new UniqueDrop(name, drop));
        raidTrackerPlugin.getFw().writeToFile(raidTracker);
    }
}