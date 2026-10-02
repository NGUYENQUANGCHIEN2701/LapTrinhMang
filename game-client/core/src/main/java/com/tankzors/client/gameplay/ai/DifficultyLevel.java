package com.tankzors.client.gameplay.ai;

/**
 * Cấu hình AI theo độ khó. Parity server ai.DifficultyLevel.
 * (speed px/s, visionRange px, reactionDelay s, shootCooldown s, accuracySpread deg)
 */
public enum DifficultyLevel {
    // Các xe AI mặc định của ba màn campaign tương ứng tank nhẹ đầu tiên
    // trong Builds, dùng Shell (type 0, sát thương 1). Độ khó chỉ thay đổi
    // hành vi và HP; không được âm thầm nâng damage lên thang cũ 25 HP.
    EASY(60, 200, 0.5, 3.5, 15.0, 3, 0),
    // Campaign 2 và 3 giữ mức độ quan sát/độ chính xác riêng, nhưng nhịp
    // di chuyển và bắn phải giống Màn 1 để không tạo cảm giác AI quá nhanh.
    MEDIUM(60, 240, 0.5, 3.5, 10.0, 3, 0),
    HARD(60, 280, 0.3, 3.5, 6.0, 5, 0);

    private final double speed;
    private final double visionRange;
    private final double reactionDelay;
    private final double shootCooldown;
    private final double accuracySpread;
    private final int referenceHitPoints;
    private final int referenceWeaponType;

    DifficultyLevel(double speed, double visionRange, double reactionDelay, double shootCooldown, double accuracySpread,
                    int referenceHitPoints, int referenceWeaponType) {
        this.speed = speed;
        this.visionRange = visionRange;
        this.reactionDelay = reactionDelay;
        this.shootCooldown = shootCooldown;
        this.accuracySpread = accuracySpread;
        this.referenceHitPoints = referenceHitPoints;
        this.referenceWeaponType = referenceWeaponType;
    }

    public double getSpeed() { return speed; }
    public double getVisionRange() { return visionRange; }
    public double getReactionDelay() { return reactionDelay; }
    public double getShootCooldown() { return shootCooldown; }
    public double getAccuracySpread() { return accuracySpread; }
    public int getReferenceHitPoints() { return referenceHitPoints; }
    public int getReferenceWeaponType() { return referenceWeaponType; }
}
