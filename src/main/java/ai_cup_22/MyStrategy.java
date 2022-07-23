package ai_cup_22;

import ai_cup_22.debugging.Color;
import ai_cup_22.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class MyStrategy {
    private static Constants constants;
    private static int bestWeaponInd;

    private static HashMap<Integer, Loot> potionMemory = new HashMap<>();
    private static HashMap<Integer, Loot> gunMemory = new HashMap<>();
    private static HashMap<Integer, Loot> ammoMemory = new HashMap<>();

    private List<Unit> enemies;
    private List<Unit> myUnits;
    private List<Loot> loot;
    private Zone zone;
    private Loot closestPotion;
    private Loot closestAmmo;
    private Loot closestAmmoForMyWeapon;
    private Loot closestCoolGun;
    private Optional<DebugInterface> debug;

    // In enemy long range aim
    // Go direct to enemy with drinking potion
    // To close to border
    // Surrounded by enemies

    public MyStrategy(ai_cup_22.model.Constants constants) {
        MyStrategy.constants = constants;
        defineBestWeapon();
    }

    private void defineBestWeapon() {
        double dps = 0;
        for (int i = 0; i < constants.getWeapons().length; i++) {
            WeaponProperties weapon = constants.getWeapons()[i];
            System.out.printf("Weapon %d%ndmg=%.2f\tpps=%.2f\tangle=%.2f%n", i, weapon.getProjectileDamage(), weapon.getRoundsPerSecond(), weapon.getSpread());
            if (dps < weapon.getRoundsPerSecond() * weapon.getProjectileDamage() * (1.0 - weapon.getSpread() / 360.0)) {
                dps = weapon.getRoundsPerSecond() * weapon.getProjectileDamage() * (1.0 - weapon.getSpread() / 360.0);
                bestWeaponInd = i;
            }
        }
    }

    public ai_cup_22.model.Order getOrder(ai_cup_22.model.Game game, DebugInterface debugInterface) {
        debug = Optional.ofNullable(debugInterface);
        Map<Integer, UnitOrder> orders = new HashMap<>();
        enemies = Stream.of(game.getUnits()).filter(u -> u.getPlayerId() != game.getMyId()).toList();
        myUnits = Stream.of(game.getUnits()).filter(u -> u.getPlayerId() == game.getMyId()).toList();
        loot = List.of(game.getLoot());
        zone = game.getZone();
        myUnits.forEach(unit -> orders.put(unit.getId(), getUnitOrder(unit)));
        return new Order(orders);
    }
    private void initUnitData(Unit unit) {
        if (potionMemory.get(unit.getId()) == null) {
            closestPotion = findClosesPotion(unit);
            potionMemory.put(unit.getId(), closestPotion);
        } else {
            Loot candidate = findClosesPotion(unit);
            closestPotion = potionMemory.get(unit.getId());
            if (candidate != null && findDistance(unit, closestPotion) > findDistance(unit, candidate)) {
                closestPotion = candidate;
                potionMemory.put(unit.getId(), candidate);
            }
        }

        closestAmmo = findClosesAmmo(unit);

        if (ammoMemory.get(unit.getId()) == null) {
            closestAmmoForMyWeapon = findAmmoForCurrentWeapon(unit);
            ammoMemory.put(unit.getId(), closestAmmoForMyWeapon);
        } else {
            Loot candidate = findAmmoForCurrentWeapon(unit);
            closestAmmoForMyWeapon = ammoMemory.get(unit.getId());
            if (candidate != null && findDistance(unit, closestAmmoForMyWeapon) > findDistance(unit, candidate)) {
                closestAmmoForMyWeapon = candidate;
                ammoMemory.put(unit.getId(), candidate);
            }
        }

        if (gunMemory.get(unit.getId()) == null) {
            closestCoolGun = findClosesCoolGun(unit);
            gunMemory.put(unit.getId(), closestCoolGun);
        } else {
            Loot candidate = findClosesCoolGun(unit);
            closestCoolGun = gunMemory.get(unit.getId());
            if (candidate != null && findDistance(unit, closestCoolGun) > findDistance(unit, candidate)) {
                closestCoolGun = candidate;
                gunMemory.put(unit.getId(), candidate);
            }
        }
    }

    private Loot findClosesCoolGun(Unit unit) {
        List<Loot> coolerGuns = loot.stream()
                .filter(l -> l.getItem() instanceof Item.Weapon)
                .filter(l -> {
                    Item.Weapon weapon = (Item.Weapon) l.getItem();
                    return weapon.getTypeIndex() == bestWeaponInd;
                }).toList();
        return findClosesLoot(unit, coolerGuns, Item.Weapon.class);
    }

    private Loot findClosesAmmo(Unit unit) {
        return findClosesLoot(unit, Item.Ammo.class);
    }

    private Loot findClosesPotion(Unit unit) {
        return findClosesLoot(unit, Item.ShieldPotions.class);
    }

    private Loot findAmmoForCurrentWeapon(Unit unit) {
        if (unit.getWeapon() == null) {
            return null;
        }
        List<Loot> fitAmmo = loot.stream()
                .filter(l -> l.getItem() instanceof Item.Ammo)
                .filter(l -> ((Item.Ammo) l.getItem()).getWeaponTypeIndex() == unit.getWeapon())
                .toList();
        return findClosesLoot(unit, fitAmmo, Item.Ammo.class);
    }

    private UnitOrder getUnitOrder(Unit unit) {
        initUnitData(unit);

        ActionOrder resultAction = null;
        Vec2 targetViewPoint = scanArea(unit);
        Vec2 targetGoPoint = goToCenter(unit);

        if (shouldILookForShield(unit)) {
            System.out.println("Go to search potions");
            if (canISeePotion()) {
                System.out.println("I see the potion, move!");
                targetGoPoint = moveToPotion(unit);
                if (canITakePotion(unit)) {
                    System.out.println("I could take potion!");
                    resultAction = new ActionOrder.Pickup(closestPotion.getId());
                    potionMemory.remove(unit.getId());
                }
            } else {
                System.out.println("Do not see potions, scan area");
            }
        }

        if (!doIHaveBestWeapon(unit)) {
            System.out.println("Maybe i could find better weapon");
            if (canISeeGun(unit)) {
                System.out.println("Cool gun in my range!");
                targetGoPoint = moveToBestWeapon(unit);
                if (canITakeBestWeapon(unit)) {
                    if (shouldIDropMyGun(unit)) {
                        if (unit.getAction() == null || unit.getAction().getActionType() != ActionType.LOOTING) {
                            System.out.println("Drop current weapon!");
                            resultAction = new ActionOrder.DropWeapon();
                            gunMemory.remove(unit.getId());
                        } else {
                            System.out.println("Drop in process");
                        }
                    } else {
                        System.out.println("Take the weapon!");
                        resultAction = new ActionOrder.Pickup(closestCoolGun.getId());
                    }
                }
            }
        }

        if (unit.getWeapon() != null && !doIHaveAmmo(unit)) {
            if (!doIHaveWeapon(unit)) {
                System.out.println("I do not heave weapon!");
                if (canISeeWeapon()) {
                    System.out.println("See weapon, move");
                    targetGoPoint = moveToWeapon(unit);
                    if (canITakeWeapon(unit)) {
                        System.out.println("Take the weapon!");
                        resultAction = new ActionOrder.Pickup(getLoot(unit));
                    }
                }
            } else {
                System.out.println("Out of ammo, need to reload!");
                if (canISeeAmmoForMyWeapon()) {
                    System.out.println("I see the ammo, move!");
                    targetGoPoint = moveToMyAmmo(unit);
                    if (canITakeMyAmmo(unit)) {
                        System.out.println("I could take the ammo!");
                        resultAction = new ActionOrder.Pickup(closestAmmoForMyWeapon.getId());
                        ammoMemory.remove(unit.getId());
                    }
                } else {
                    System.out.println("Do not see ammo, scan area");
                }
            }
        }

        if (unit.getWeapon() != null && amISpawned(unit) && isISeeEnemy(unit) && resultAction == null) {
            System.out.println("I see an enemy!");
            if (doIHaveAmmo(unit)) {
                boolean iCanShoot = canIShoot(unit);
                boolean enemyInTarget = areEnemyInTarget(unit);
                boolean enemyIsCloseEnough = areEnemyIsClose(unit);
                System.out.printf("Shoot = %s, in target = %s, is close = %s%n", iCanShoot, enemyInTarget, enemyIsCloseEnough);
                if (enemyIsCloseEnough) {
                    targetViewPoint = makeAim(unit);
                    resultAction = new ActionOrder.Aim(iCanShoot && enemyInTarget);
                }/* else if (!enemyIsCloseEnough) {
                    System.out.println("Go closer to enemy!");
                    targetGoPoint = makeAim(unit);
                }*/
            }
        }

        if (shouldIDrinkShield(unit) && couldIDrinkShield(unit)) {
            System.out.println("I should drink potion!");
            resultAction = new ActionOrder.UseShieldPotion();
        }

        if (!isHealthOptimal(unit)) {
            System.out.println("Need to retreat!");
            targetGoPoint = goToMedianCenter(unit);
            targetViewPoint = targetGoPoint;
            if (resultAction instanceof ActionOrder.Aim) {
                resultAction = null;
            }
        }

        if (unit.getAction() != null) {
            System.out.println("Already have an action " + unit.getAction().toString());
            resultAction = null;
        }

        System.out.printf("My orders:\n Go to\tX: %.2f\tY: %.2f\n View point\tX: %.2f\tY: %.2f\n Action is %s\n", targetGoPoint.getX(), targetGoPoint.getY(), targetViewPoint.getX(), targetViewPoint.getY(), resultAction != null ? resultAction.toString() : "null");

        return new UnitOrder(targetGoPoint, targetViewPoint, resultAction);
    }

    private boolean shouldIDropMyGun(Unit unit) {
        return unit.getWeapon() != null && unit.getWeapon() != bestWeaponInd;
    }
    private boolean doIHaveBestWeapon(Unit unit) {
        System.out.printf("My weapon is %d, the best weapon is %d%n", unit.getWeapon(), bestWeaponInd);
        return unit.getWeapon() != null && bestWeaponInd == unit.getWeapon();
    }

    private boolean isHealthOptimal(Unit unit) {
        System.out.printf("Current health %.2f , max health is %.2f%n", unit.getHealth(), constants.getUnitHealth());
        return unit.getHealth() * 1.5 >= constants.getUnitHealth();
    }

    private boolean doIHaveWeapon(Unit unit) {
        return unit.getWeapon() != null;
    }

    /**
     * Return view direction to scan area.
     */
    private Vec2 scanArea(Unit unit) {
        return new Vec2(-unit.getDirection().getY(), unit.getDirection().getX());
    }

    /**
     * Return closest loot to the Unit.
     */
    private int getLoot(Unit unit) {
        double distance = Integer.MAX_VALUE;
        Loot bestLoot = null;
        for (Loot l1 : loot) {
            if (findDistance(l1.getPosition(), unit.getPosition()) < distance) {
                distance = findDistance(l1.getPosition(), unit.getPosition());
                bestLoot = l1;
            }
        }
        return bestLoot.getId();
    }

    /**
     * ?????????????? ? ????????? ?????????? ?????? ? ??????, ???? ???? ????????? ??? ?????????.
     * ???????? ????????? ???????, ?? ???? ???? ????????? ??? ????????? ???? ?????????? ?? ?????? ????? ?? ????????
     * ?????? ??? ?????? ?????.
     */
    private boolean canITakePotion(Unit unit) {
        return findDistance(unit, closestPotion) < constants.getUnitRadius();
    }

    private boolean canITakeMyAmmo(Unit unit) {
        return findDistance(unit, closestAmmoForMyWeapon) < constants.getUnitRadius();
    }
    private boolean canITakeBestWeapon(Unit unit) {
        return findDistance(unit, closestCoolGun) < constants.getUnitRadius();
    }

    private boolean canITakeWeapon(Unit unit) {
        Loot loot = findClosesLoot(unit, Item.Weapon.class);
        return findDistance(unit, loot) < constants.getUnitRadius();
    }


    private Loot findClosesLoot(Unit unit, Class<? extends Item> lootType) {
        return findClosesLoot(unit, loot, lootType);
    }
    private Loot findClosesLoot(Unit unit, List<Loot> loot, Class<? extends Item> lootType) {
        double distance = Integer.MAX_VALUE;
        Loot bestLoot = null;
        List<Loot> filteredLoot = loot.stream().filter(l -> lootType.isInstance(l.getItem())).toList();
        for (Loot l1 : filteredLoot) {
            if (findDistance(l1.getPosition(), unit.getPosition()) < distance) {
                distance = findDistance(l1.getPosition(), unit.getPosition());
                bestLoot = l1;
            }
        }
        return bestLoot;
    }

    private boolean canISeeAmmoForMyWeapon() {
        return closestAmmoForMyWeapon != null;
    }

    private boolean canISeeGun(Unit unit) {
        return closestCoolGun != null;
    }

    private boolean canISeePotion() {
        return closestPotion != null;
    }

    private boolean canISeeWeapon() {
        return loot.stream().anyMatch(l -> l.getItem() instanceof Item.Weapon);
    }

    private boolean shouldILookForShield(Unit unit) {
        System.out.printf("Currently potions %d, could carry %d%n", unit.getShieldPotions(), constants.getMaxShieldPotionsInInventory());
        return unit.getShieldPotions() < constants.getMaxShieldPotionsInInventory();
    }

    private boolean amISpawned(Unit unit) {
        return unit.getRemainingSpawnTime() == null;
    }

    private boolean doIHaveAmmo(Unit unit) {
        System.out.println("Remain ammo " +  unit.getAmmo()[unit.getWeapon()]);
        return unit.getAmmo()[unit.getWeapon()] > 12;
    }

    private boolean areEnemyIsClose(Unit unit) {
        Unit target = findBestEnemyTarget(unit);
        double distance = findDistance(unit, target);
        WeaponProperties weaponProperties = constants.getWeapons()[unit.getWeapon()];
        return distance <= weaponProperties.getProjectileSpeed() * weaponProperties.getProjectileLifeTime();
    }

    private boolean areEnemyInTarget(Unit unit) {
        Unit target = findBestEnemyTarget(unit);
        double distance = findDistance(unit, target);
        double angle = findAngleBySin(distance, constants.getUnitRadius());
// TODO
        return true;
    }
    private double findDistance(Vec2 first, Vec2 second) {
        return Math.sqrt(Math.pow(first.getX() - second.getX(), 2.0) + Math.pow(first.getY() - second.getY(), 2.0));
    }

    private double findDistance(Unit unit, Unit target) {
        return findDistance(unit.getPosition(), target.getPosition());
    }

    private double findDistance(Unit unit, Loot loot) {
        return findDistance(unit.getPosition(), loot.getPosition());
    }

    private double findAngleBySin(double distance, double unitRadius) {
        return Math.asin(unitRadius / distance);
    }

    private Vec2 makeAim(Unit unit) {
        Unit target = findBestEnemyTarget(unit);
        return moveToTarget(unit, target);
    }

    private Unit findBestEnemyTarget(Unit unit) {
        double distance = Integer.MAX_VALUE;
        Unit enemy = null;
        for (Unit e : enemies) {
            if (findDistance(e.getPosition(), unit.getPosition()) < distance) {
                distance = findDistance(e.getPosition(), unit.getPosition());
                enemy = e;
            }
        }
        return enemy;
    }

    private boolean canIShoot(Unit unit) {
        return unit.getAim() == 1.0;
    }

    private boolean isISeeEnemy(Unit unit) {
        return !enemies.isEmpty();
    }

    private boolean couldIDrinkShield(Unit unit) {
        return unit.getShieldPotions() > 0;
    }

    private boolean shouldIDrinkShield(Unit unit) {
        return unit.getShield() + constants.getShieldPerPotion() < constants.getMaxShield();
    }

    private boolean haveIDrinkPotion(Unit unit, ActionOrder resultAction) {
        return unit.getShield() < (constants.getMaxShield() * 0.6) || !(resultAction instanceof ActionOrder.Aim);
    }

    ////// Moving functions

    private Vec2 moveToMyAmmo(Unit unit) {
        debug.ifPresent(d -> d.addSegment(unit.getPosition(), closestAmmoForMyWeapon.getPosition(), 0.5, new Color(0, 1.0, 0, 0.5)));
        return moveToTarget(unit.getPosition(), closestAmmoForMyWeapon);
    }
    private Vec2 moveToPotion(Unit unit) {
        debug.ifPresent(d -> d.addSegment(unit.getPosition(), closestPotion.getPosition(), 1.0, new Color(0, 0, 1.0, 0.75)));
        return moveToTarget(unit.getPosition(), closestPotion);
    }

    private Vec2 moveToBestWeapon(Unit unit) {
        debug.ifPresent(d -> d.addSegment(unit.getPosition(), closestCoolGun.getPosition(), 0.5, new Color(1.0, 0, 0, 0.5)));
        return moveToTarget(unit.getPosition(), closestCoolGun);
    }
    private Vec2 moveToWeapon(Unit unit) {
        Loot loot = findClosesLoot(unit, Item.Weapon.class);
        return moveToTarget(unit.getPosition(), loot.getPosition());
    }
    private Vec2 moveToTarget(Unit position, Unit target) {
        return moveToTarget(position.getPosition(), target.getPosition());
    }
    private Vec2 moveToTarget(Vec2 position, Loot target) {
        return moveToTarget(position, target.getPosition());
    }
    private Vec2 moveToTarget(Vec2 position, Vec2 target) {
        return new Vec2(target.getX() - position.getX(), target.getY() - position.getY());
    }

    private Vec2 goToCenter(Unit unit) {
        return moveToTarget(unit.getPosition(), zone.getNextCenter());
    }
    private Vec2 goToMedianCenter(Unit unit) {
        return moveToTarget(unit.getPosition(), findMedianCenter());
    }

    private Vec2 findMedianCenter() {
        return new Vec2(
                (zone.getCurrentCenter().getX() + zone.getNextCenter().getX()) * 0.75,
                (zone.getCurrentCenter().getY() + zone.getNextCenter().getY()) * 0.75);
    }

    public void debugUpdate(int displayedTick, DebugInterface debugInterface) {

    }

    public void finish() {
    }
}