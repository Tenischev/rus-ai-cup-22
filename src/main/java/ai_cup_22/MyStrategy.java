package ai_cup_22;

import ai_cup_22.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class MyStrategy {
    private static Constants constants;

    private List<Unit> enemies;
    private List<Unit> myUnits;
    private List<Loot> loot;
    private Zone zone;

    public MyStrategy(ai_cup_22.model.Constants constants) {
        MyStrategy.constants = constants;
    }

    public ai_cup_22.model.Order getOrder(ai_cup_22.model.Game game, DebugInterface debugInterface) {
        java.util.HashMap<Integer, UnitOrder> orders = new java.util.HashMap<>();
        enemies = Stream.of(game.getUnits()).filter(u -> u.getPlayerId() != game.getMyId()).toList();
        myUnits = Stream.of(game.getUnits()).filter(u -> u.getPlayerId() == game.getMyId()).toList();
        loot = List.of(game.getLoot());
        zone = game.getZone();
        myUnits.forEach(unit -> orders.put(unit.getId(), getUnitOrder(unit)));
        return new Order(orders);
    }

    private UnitOrder getUnitOrder(Unit unit) {
        ActionOrder resultAction = null;
        Vec2 targetViewPoint = scanArea(unit);
        Vec2 targetGoPoint = goToCenter(unit);

        boolean lookForShield = shouldILookForShield(unit);
        if (lookForShield) {
            System.out.println("Go to search potions");
            boolean iSeePotion = canISeePotion(unit);
            if (iSeePotion) {
                System.out.println("I see at least 1 potion!");
                targetGoPoint = moveToPotion(unit);
                boolean iCanTakePotion = canITakePotion(unit);
                if (iCanTakePotion) {
                    System.out.println("I could take potion!");
                    resultAction = new ActionOrder.Pickup(getLoot(unit));
                }
            } else {
                System.out.println("Do not see potions, scan area");
            }
        }

        boolean iHaveWeapon = doIHaveWeapon(unit);
        if (!iHaveWeapon) {
            System.out.println("I do not heave weapon!");
            boolean iSeeWeapon = canISeeWeapon(unit);
            if (iSeeWeapon) {
                System.out.println("See weapon, move");
                targetGoPoint = moveToWeapon(unit);
                boolean iCanTakeWeapon = canITakeWeapon(unit);
                if (iCanTakeWeapon) {
                    System.out.println("Take the weapon!");
                    resultAction = new ActionOrder.Pickup(getLoot(unit));
                }
            }
        }

        boolean iSpawned = amISpawned(unit);
        boolean iSeeEnemy = isISeeEnemy(unit);
        if (iSpawned && iSeeEnemy) {
            System.out.println("I see enemy!");
            boolean outOfAmmo = !doIHaveAmmo(unit);
            if (outOfAmmo) {
                System.out.println("Out of ammo, need to reload!");
                UnitOrder bestAction = findAmmoOrWeapon(unit);
                targetViewPoint = bestAction.getTargetDirection();
                targetGoPoint = bestAction.getTargetVelocity();
                resultAction = bestAction.getAction();
            } else {
                targetViewPoint = makeAim(unit);
                boolean iCanShoot = canIShoot(unit);
                boolean enemyInTarget = areEnemyInTarget(unit);
                System.out.println("Shoot = " + (iCanShoot && enemyInTarget));
                resultAction = new ActionOrder.Aim(iCanShoot && enemyInTarget);
            }
        }

        boolean iShouldDrinkShield = shouldIDrinkShield(unit);
        boolean iCouldDrinkShield = couldIDrinkShield(unit);
        if (iShouldDrinkShield && iCouldDrinkShield) {
            System.out.println("I should drink potion!");
            resultAction = new ActionOrder.UseShieldPotion();
        }

        if (unit.getAction() != null) {
            System.out.println("Already have an action " + unit.getAction().toString());
            resultAction = null;
        }

        System.out.printf("My orders:\n Go to\tX: %.2f\tY: %.2f\n View point\tX: %.2f\tY: %.2f\n Action is %s\n", targetGoPoint.getX(), targetGoPoint.getY(), targetViewPoint.getX(), targetViewPoint.getY(), resultAction != null ? resultAction.toString() : "null");

        return new UnitOrder(targetGoPoint, targetViewPoint, resultAction);
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
        Loot loot = findClosesLoot(unit, Item.ShieldPotions.class);
        return findDistance(unit, loot) < constants.getUnitRadius();
    }

    private boolean canITakeAmmo(Unit unit) {
        Loot loot = findClosesLoot(unit, Item.Ammo.class);
        return findDistance(unit, loot) < constants.getUnitRadius();
    }

    private boolean canITakeWeapon(Unit unit) {
        Loot loot = findClosesLoot(unit, Item.Weapon.class);
        return findDistance(unit, loot) < constants.getUnitRadius();
    }

    private Vec2 moveToPotion(Unit unit) {
        Loot loot = findClosesLoot(unit, Item.ShieldPotions.class);
        return targetTo(unit.getPosition(), loot.getPosition());
    }

    private Vec2 moveToWeapon(Unit unit) {
        Loot loot = findClosesLoot(unit, Item.Weapon.class);
        return targetTo(unit.getPosition(), loot.getPosition());
    }

    private Vec2 targetTo(Vec2 position, Vec2 target) {
        return new Vec2(target.getX() - position.getX(), target.getY() - position.getY());
    }

    private Loot findClosesLoot(Unit unit, Class<? extends Item> lootType) {
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

    private boolean canISeePotion(Unit unit) {
        return loot.stream().anyMatch(l -> l.getItem() instanceof Item.ShieldPotions);
    }

    private boolean canISeeWeapon(Unit unit) {
        return loot.stream().anyMatch(l -> l.getItem() instanceof Item.Weapon);
    }

    private boolean shouldILookForShield(Unit unit) {
        return unit.getShieldPotions() < constants.getMaxShieldPotionsInInventory();
    }

    private boolean amISpawned(Unit unit) {
        return unit.getRemainingSpawnTime() == null;
    }

    private UnitOrder findAmmoOrWeapon(Unit unit) {
        Vec2 target = goToCenter(unit);
        ActionOrder actionOrder = null;
        Loot ammoForMyWeapon = findAmmoForCurrentWeapon(unit);
        if (ammoForMyWeapon != null) {
            System.out.println("Move to ammo for my weapon!");
            target = targetTo(unit.getPosition(), ammoForMyWeapon.getPosition());
        } else {
            System.out.println("No ammo for my weapon!");
            // TODO
        }
        if (canITakeAmmo(unit)) {
            System.out.println("Take the ammo!");
            actionOrder = new ActionOrder.Pickup(getLoot(unit));
        }
        return new UnitOrder(
                target,
                new Vec2(zone.getNextCenter().getX(), zone.getNextCenter().getY()),
                actionOrder);
    }

    private Loot findAmmoForCurrentWeapon(Unit unit) {
        List<Loot> fitAmmo = loot.stream()
                .filter(l -> l.getItem() instanceof Item.Ammo)
                .filter(l -> ((Item.Ammo) l.getItem()).getWeaponTypeIndex() == unit.getWeapon())
                .toList();
        double distance = Integer.MAX_VALUE;
        Loot closestAmmo = null;
        for (Loot e : fitAmmo) {
            if (findDistance(e.getPosition(), unit.getPosition()) < distance) {
                distance = findDistance(e.getPosition(), unit.getPosition());
                closestAmmo = e;
            }
        }
        return closestAmmo;
    }

    private Vec2 goToCenter(Unit unit) {
        return new Vec2(zone.getNextCenter().getX() - unit.getPosition().getX(), zone.getNextCenter().getY() - unit.getPosition().getY());
    }

    private boolean doIHaveAmmo(Unit unit) {
        System.out.println("Remain ammo " +  unit.getAmmo()[unit.getWeapon()]);
        return unit.getAmmo()[unit.getWeapon()] > 0;
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
        return new Vec2(target.getPosition().getX(), target.getPosition().getY());
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

    public void debugUpdate(int displayedTick, DebugInterface debugInterface) {
    }

    public void finish() {
    }
}