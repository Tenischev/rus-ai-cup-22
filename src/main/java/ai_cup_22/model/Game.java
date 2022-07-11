package ai_cup_22.model;

import ai_cup_22.util.StreamUtil;

/**
 * Current game's state
 */
public class Game {
    /**
     * Your player's id
     */
    private int myId;

    /**
     * Your player's id
     */
    public int getMyId() {
        return myId;
    }

    /**
     * Your player's id
     */
    public void setMyId(int value) {
        this.myId = value;
    }
    /**
     * List of players (teams)
     */
    private Player[] players;

    /**
     * List of players (teams)
     */
    public Player[] getPlayers() {
        return players;
    }

    /**
     * List of players (teams)
     */
    public void setPlayers(Player[] value) {
        this.players = value;
    }
    /**
     * Current tick
     */
    private int currentTick;

    /**
     * Current tick
     */
    public int getCurrentTick() {
        return currentTick;
    }

    /**
     * Current tick
     */
    public void setCurrentTick(int value) {
        this.currentTick = value;
    }
    /**
     * List of units visible by your team
     */
    private Unit[] units;

    /**
     * List of units visible by your team
     */
    public Unit[] getUnits() {
        return units;
    }

    /**
     * List of units visible by your team
     */
    public void setUnits(Unit[] value) {
        this.units = value;
    }
    /**
     * List of loot visible by your team
     */
    private Loot[] loot;

    /**
     * List of loot visible by your team
     */
    public Loot[] getLoot() {
        return loot;
    }

    /**
     * List of loot visible by your team
     */
    public void setLoot(Loot[] value) {
        this.loot = value;
    }
    /**
     * List of projectiles visible by your team
     */
    private Projectile[] projectiles;

    /**
     * List of projectiles visible by your team
     */
    public Projectile[] getProjectiles() {
        return projectiles;
    }

    /**
     * List of projectiles visible by your team
     */
    public void setProjectiles(Projectile[] value) {
        this.projectiles = value;
    }
    /**
     * Current state of game zone
     */
    private Zone zone;

    /**
     * Current state of game zone
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Current state of game zone
     */
    public void setZone(Zone value) {
        this.zone = value;
    }
    /**
     * List of sounds heard by your team during last tick
     */
    private Sound[] sounds;

    /**
     * List of sounds heard by your team during last tick
     */
    public Sound[] getSounds() {
        return sounds;
    }

    /**
     * List of sounds heard by your team during last tick
     */
    public void setSounds(Sound[] value) {
        this.sounds = value;
    }

    public Game(int myId, Player[] players, int currentTick, Unit[] units, Loot[] loot, Projectile[] projectiles, Zone zone, Sound[] sounds) {
        this.myId = myId;
        this.players = players;
        this.currentTick = currentTick;
        this.units = units;
        this.loot = loot;
        this.projectiles = projectiles;
        this.zone = zone;
        this.sounds = sounds;
    }

    /**
     * Read Game from input stream
     */
    public static Game readFrom(java.io.InputStream stream) throws java.io.IOException {
        int myId;
        myId = StreamUtil.readInt(stream);
        Player[] players;
        players = new Player[StreamUtil.readInt(stream)];
        for (int playersIndex = 0; playersIndex < players.length; playersIndex++) {
            Player playersElement;
            playersElement = Player.readFrom(stream);
            players[playersIndex] = playersElement;
        }
        int currentTick;
        currentTick = StreamUtil.readInt(stream);
        Unit[] units;
        units = new Unit[StreamUtil.readInt(stream)];
        for (int unitsIndex = 0; unitsIndex < units.length; unitsIndex++) {
            Unit unitsElement;
            unitsElement = Unit.readFrom(stream);
            units[unitsIndex] = unitsElement;
        }
        Loot[] loot;
        loot = new Loot[StreamUtil.readInt(stream)];
        for (int lootIndex = 0; lootIndex < loot.length; lootIndex++) {
            Loot lootElement;
            lootElement = Loot.readFrom(stream);
            loot[lootIndex] = lootElement;
        }
        Projectile[] projectiles;
        projectiles = new Projectile[StreamUtil.readInt(stream)];
        for (int projectilesIndex = 0; projectilesIndex < projectiles.length; projectilesIndex++) {
            Projectile projectilesElement;
            projectilesElement = Projectile.readFrom(stream);
            projectiles[projectilesIndex] = projectilesElement;
        }
        Zone zone;
        zone = Zone.readFrom(stream);
        Sound[] sounds;
        sounds = new Sound[StreamUtil.readInt(stream)];
        for (int soundsIndex = 0; soundsIndex < sounds.length; soundsIndex++) {
            Sound soundsElement;
            soundsElement = Sound.readFrom(stream);
            sounds[soundsIndex] = soundsElement;
        }
        return new Game(myId, players, currentTick, units, loot, projectiles, zone, sounds);
    }

    /**
     * Write Game to output stream
     */
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        StreamUtil.writeInt(stream, myId);
        StreamUtil.writeInt(stream, players.length);
        for (Player playersElement : players) {
            playersElement.writeTo(stream);
        }
        StreamUtil.writeInt(stream, currentTick);
        StreamUtil.writeInt(stream, units.length);
        for (Unit unitsElement : units) {
            unitsElement.writeTo(stream);
        }
        StreamUtil.writeInt(stream, loot.length);
        for (Loot lootElement : loot) {
            lootElement.writeTo(stream);
        }
        StreamUtil.writeInt(stream, projectiles.length);
        for (Projectile projectilesElement : projectiles) {
            projectilesElement.writeTo(stream);
        }
        zone.writeTo(stream);
        StreamUtil.writeInt(stream, sounds.length);
        for (Sound soundsElement : sounds) {
            soundsElement.writeTo(stream);
        }
    }

    /**
     * Get string representation of Game
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Game { ");
        stringBuilder.append("myId: ");
        stringBuilder.append(String.valueOf(myId));
        stringBuilder.append(", ");
        stringBuilder.append("players: ");
        stringBuilder.append("[ ");
        for (int playersIndex = 0; playersIndex < players.length; playersIndex++) {
            if (playersIndex != 0) {
                stringBuilder.append(", ");
            }
            Player playersElement = players[playersIndex];
            stringBuilder.append(String.valueOf(playersElement));
        }
        stringBuilder.append(" ]");
        stringBuilder.append(", ");
        stringBuilder.append("currentTick: ");
        stringBuilder.append(String.valueOf(currentTick));
        stringBuilder.append(", ");
        stringBuilder.append("units: ");
        stringBuilder.append("[ ");
        for (int unitsIndex = 0; unitsIndex < units.length; unitsIndex++) {
            if (unitsIndex != 0) {
                stringBuilder.append(", ");
            }
            Unit unitsElement = units[unitsIndex];
            stringBuilder.append(String.valueOf(unitsElement));
        }
        stringBuilder.append(" ]");
        stringBuilder.append(", ");
        stringBuilder.append("loot: ");
        stringBuilder.append("[ ");
        for (int lootIndex = 0; lootIndex < loot.length; lootIndex++) {
            if (lootIndex != 0) {
                stringBuilder.append(", ");
            }
            Loot lootElement = loot[lootIndex];
            stringBuilder.append(String.valueOf(lootElement));
        }
        stringBuilder.append(" ]");
        stringBuilder.append(", ");
        stringBuilder.append("projectiles: ");
        stringBuilder.append("[ ");
        for (int projectilesIndex = 0; projectilesIndex < projectiles.length; projectilesIndex++) {
            if (projectilesIndex != 0) {
                stringBuilder.append(", ");
            }
            Projectile projectilesElement = projectiles[projectilesIndex];
            stringBuilder.append(String.valueOf(projectilesElement));
        }
        stringBuilder.append(" ]");
        stringBuilder.append(", ");
        stringBuilder.append("zone: ");
        stringBuilder.append(String.valueOf(zone));
        stringBuilder.append(", ");
        stringBuilder.append("sounds: ");
        stringBuilder.append("[ ");
        for (int soundsIndex = 0; soundsIndex < sounds.length; soundsIndex++) {
            if (soundsIndex != 0) {
                stringBuilder.append(", ");
            }
            Sound soundsElement = sounds[soundsIndex];
            stringBuilder.append(String.valueOf(soundsElement));
        }
        stringBuilder.append(" ]");
        stringBuilder.append(" }");
        return stringBuilder.toString();
    }
}