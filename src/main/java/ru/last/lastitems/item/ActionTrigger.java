package ru.last.lastitems.item;

public enum ActionTrigger {
    // 0.1 - 0.2.1 triggers
    ON_PROJECTILE_THROW,  // EntityShootBowEvent, PlayerLaunchProjectileEvent
    ON_PROJECTILE_IMPACT, // ProjectileHitEvent
    ON_RIGHT_CLICK,       // PlayerInteractEvent (RIGHT_CLICK)
    ON_LEFT_CLICK,        // PlayerInteractEvent (LEFT_CLICK)
    ON_HIT,               // EntityDamageByEntityEvent ()
    ON_KILL_ENTITY,       // EntityDeathEvent (for entities killed)
    ON_KILL_PLAYER,       // EntityDeathEvent (for players killed)
    ON_SWAPPING,          // PlayerSwapHandItemsEvent
    ON_BLOCK_BREAK,       // BlockBreakEvent
    ON_BLOCK_PLACE,       // BlockPlaceEvent
    ON_INTERACT,          // PlayerInteractEvent (any interaction, can be used for more generic triggers)
    
    // 0.2.2+ triggers
    ON_CONSUME,           // PlayerItemConsumeEvent
    ON_FISH,              // PlayerFishEvent
    ON_SNEAK,             // PlayerToggleSneakEvent
    ON_SPRINT,            // PlayerToggleSprintEvent
    ON_JUMP,              // PlayerMoveEvent (Y change)
    ON_DROP,              // PlayerDropItemEvent
    ON_PICKUP,            // EntityPickupItemEvent
    ON_EQUIP,             // Custom logic for armor/hand equip
    ON_WORLD_CHANGE,      // PlayerChangedWorldEvent
    ON_JOIN,              // PlayerJoinEvent
    ON_QUIT,              // PlayerQuitEvent
    ON_DEATH,             // EntityDeathEvent (for the player having the item)
    ON_RESPAWN,           // PlayerRespawnEvent
    ON_BOW_SHOOT,         // EntityShootBowEvent
    ON_TELEPORT,          // PlayerTeleportEvent
    ON_EXP_CHANGE,        // PlayerExpChangeEvent
    ON_LEVEL_CHANGE,      // PlayerLevelChangeEvent
    ON_BED_ENTER,         // PlayerBedEnterEvent
    ON_BED_LEAVE,         // PlayerBedLeaveEvent
    ON_SHEAR,             // PlayerShearEntityEvent
    ON_BUCKET_FILL,       // PlayerBucketFillEvent
    ON_BUCKET_EMPTY,      // PlayerBucketEmptyEvent
    ON_ITEM_BREAK,        // PlayerItemBreakEvent
    ON_ITEM_MEND,         // PlayerItemMendEvent

    // 0.2.5+ triggers
    ON_SHIFT_LEFT_CLICK,  // PlayerInteractEvent (Shift + LEFT_CLICK)
    ON_SHIFT_RIGHT_CLICK, // PlayerInteractEvent (Shift + RIGHT_CLICK)
    ON_ITEM_SLOT          // Tick task for held items in specific slots
}