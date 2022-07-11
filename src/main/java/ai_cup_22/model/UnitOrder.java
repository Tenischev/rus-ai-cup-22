package ai_cup_22.model;

import ai_cup_22.util.StreamUtil;

/**
 * Order for specific unit
 */
public class UnitOrder {
    /**
     * Target moving velocity
     */
    private Vec2 targetVelocity;

    /**
     * Target moving velocity
     */
    public Vec2 getTargetVelocity() {
        return targetVelocity;
    }

    /**
     * Target moving velocity
     */
    public void setTargetVelocity(Vec2 value) {
        this.targetVelocity = value;
    }
    /**
     * Target view direction (vector length doesn't matter)
     */
    private Vec2 targetDirection;

    /**
     * Target view direction (vector length doesn't matter)
     */
    public Vec2 getTargetDirection() {
        return targetDirection;
    }

    /**
     * Target view direction (vector length doesn't matter)
     */
    public void setTargetDirection(Vec2 value) {
        this.targetDirection = value;
    }
    /**
     * Order to perform an action, or None
     */
    private ActionOrder action;

    /**
     * Order to perform an action, or None
     */
    public ActionOrder getAction() {
        return action;
    }

    /**
     * Order to perform an action, or None
     */
    public void setAction(ActionOrder value) {
        this.action = value;
    }

    public UnitOrder(Vec2 targetVelocity, Vec2 targetDirection, ActionOrder action) {
        this.targetVelocity = targetVelocity;
        this.targetDirection = targetDirection;
        this.action = action;
    }

    /**
     * Read UnitOrder from input stream
     */
    public static UnitOrder readFrom(java.io.InputStream stream) throws java.io.IOException {
        Vec2 targetVelocity;
        targetVelocity = Vec2.readFrom(stream);
        Vec2 targetDirection;
        targetDirection = Vec2.readFrom(stream);
        ActionOrder action;
        if (StreamUtil.readBoolean(stream)) {
            action = ActionOrder.readFrom(stream);
        } else {
            action = null;
        }
        return new UnitOrder(targetVelocity, targetDirection, action);
    }

    /**
     * Write UnitOrder to output stream
     */
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        targetVelocity.writeTo(stream);
        targetDirection.writeTo(stream);
        if (action == null) {
            StreamUtil.writeBoolean(stream, false);
        } else {
            StreamUtil.writeBoolean(stream, true);
            action.writeTo(stream);
        }
    }

    /**
     * Get string representation of UnitOrder
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("UnitOrder { ");
        stringBuilder.append("targetVelocity: ");
        stringBuilder.append(String.valueOf(targetVelocity));
        stringBuilder.append(", ");
        stringBuilder.append("targetDirection: ");
        stringBuilder.append(String.valueOf(targetDirection));
        stringBuilder.append(", ");
        stringBuilder.append("action: ");
        stringBuilder.append(String.valueOf(action));
        stringBuilder.append(" }");
        return stringBuilder.toString();
    }
}