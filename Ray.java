package finalproject;

import java.awt.geom.Point2D;

public class Ray {
    private final Point2D.Double rayOrigin;
    private final Point2D.Double hitPosition;
    private final double rayAngle;
    public Ray(Point2D.Double rayOrigin, Point2D.Double hitPosition, double rayAngle) {
        this.rayOrigin = rayOrigin;
        this.hitPosition = hitPosition;
        this.rayAngle = rayAngle;
    }
    public Point2D getRayOrigin() {
        return rayOrigin;
    }

    public Point2D getHitPosition() {
        return hitPosition;
    }

    public double getRayAngle() {
        return rayAngle;
    }

    public double distance() {
        return rayOrigin.distance(hitPosition);
    }
}
