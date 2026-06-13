package Proxima.content;

import Proxima.units.ProtectedUnitType;
import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.BuilderAI;
import mindustry.ai.types.FlyingAI;
import mindustry.ai.types.MinerAI;
import mindustry.audio.SoundLoop;
import mindustry.ai.types.CommandAI;
import mindustry.content.Fx;
import mindustry.content.Items;
import arc.math.Mathf;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.part.DrawPart.PartProgress;
import mindustry.entities.part.HaloPart;
import mindustry.entities.part.RegionPart;
import mindustry.entities.part.ShapePart;
import mindustry.entities.pattern.*;
import mindustry.entities.units.WeaponMount;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.MultiPacker;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.weapons.PointDefenseWeapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.Env;
import Proxima.ProximaPal;
import Proxima.effects.ProximaFX;
import Proxima.expand.abilities.AttackSlowAbility;
import Proxima.expand.abilities.BoostAbility;
import Proxima.expand.bullets.AccelBulletType;
import Proxima.expand.units.AncientEngine;
import Proxima.expand.bullets.ChainBulletType;
import Proxima.expand.bullets.DelayedPointBulletType;
import Proxima.expand.util.PosLightning;

import static mindustry.Vars.*;

/**
 * 比邻星单位类型定义
 */
public class ProximaUnitTypes {

    public static UnitType proximaCoreMech;
    public static UnitType proximaTrain;
    public static UnitType ProtectedUnit;
    public static UnitType sprout;

    public static void load() {
        proximaCoreMech = new UnitType("proxima-core-mech") {{
            // 构造器
            constructor = UnitEntity::create;

            // 基础配置
            controller = u -> u.team.isAI() ? new BuilderAI(true, 400f) : new CommandAI();
            isEnemy = false;
            range = 240f;

            // 攻击时减速 ability: 正常速度3f, 攻击时0.1f, 等待3秒后恢复
            //abilities.add(new AttackSlowAbility(3f, 1f, 120f));

            // 助推尾焰 ability: 速度倍率3f, 角度锥形5f
            abilities.add(new BoostAbility(3f, 5f));

            // 空中单位配置
            targetBuildingsMobile = false;
            lowAltitude = true;
            flying = true;

            // 建造能力
            mineSpeed = 6.5f;
            mineTier = 1;
            buildSpeed = 1.0f;

            // 移动参数
            drag = 0.05f;
            speed = 4.5f;
            rotateSpeed = 15f;
            accel = 0.1f;

            // 感知范围
            fogRadius = 0f;

            // 物品容量
            itemCapacity = 30;

            // 生命值
            health = 250f;

            // 引擎 - 使用 AncientEngine
            engineOffset = 6f;
            engineSize = -1;

            // 添加多个引擎，形成复杂的尾焰效果
            engines.add(new AncientEngine(-2f, -7.5f, 1f, -90));
            engines.add(new AncientEngine(2f, -7.5f, 1f, -90));
            engines.add(new AncientEngine(0f, -8.5f, 1.5f, -90, 0.45f, 0.6f, 2.6f));

            // 大小 - 1.3格大小
            hitSize = 10.4f;

            // 总是解锁
            alwaysUnlocked = true;

            // 死亡和碰撞音效音量
            wreckSoundVolume = 0.8f;
            deathSoundVolume = 0.7f;

            // 武器 - 导弹发射器

            // 主炮 - 定点激光炮
            weapons.add(new Weapon("proxima-core-cannon") {{
                x = 0f;
                y = 0f;

                // 武器配置 - 不可旋转
                mirror = false;
                rotate = false;
                reload = 120f;
                recoil = 1.25f;
                minWarmup = 0.935f;
                cooldownTime = reload - 30f;
                shootY = 1.5f;

                // 音效
                shootSound = Sounds.shootLaser;

                // 子弹类型 - 定点延迟激光
                bullet = new DelayedPointBulletType() {{
                    width = 10f;
                    damage = 80;
                    hitColor = ProximaPal.ancientLightMid;
                    lightColor = lightningColor = trailColor = ProximaPal.ancientLightMid;
                    rangeOverride = 240f;

                    trailEffect = Fx.none;

                    hitEffect = Fx.hitLaserBlast;
                    despawnEffect = Fx.smokeCloud;

                    status = StatusEffects.melting;
                    statusDuration = 600f;

                    despawnShake = hitShake = 2f;
                    collidesAir = collidesGround = true;

                    fragBullets = 1;
                    fragBullet = new ChainBulletType(80) {{
                        length = 0;
                        collidesAir = collidesGround = true;
                        quietShoot = true;
                        hitColor = ProximaPal.ancientLightMid;
                        lightColor = lightningColor = trailColor = ProximaPal.lightSkyMiddle;
                        thick = 7f;
                        maxHit = 3;
                        hitEffect = Fx.chainLightning;
                        effectController = (f, t) -> {
                            PosLightning.createEffect(f, t, hitColor, boltNum, thick);
                        };
                    }};
                }};
            }});
        }};

        // 列车单位
        proximaTrain = new UnitType("proxima-train") {{
            // 基础配置
            constructor = UnitEntity::create;
            isEnemy = false;
            flying = false;
            
            // 移动参数
            speed = 6f;
            hitSize = 8f;
            health = 500f;
            armor = 5f;
            drag = 0.3f;
            accel = 0.5f;
            rotateSpeed = 5f;
            
            // 控制
            canBoost = false;
            logicControllable = true;
            playerControllable = true;
            allowedInPayloads = false;
            
            // 物品容量
            itemCapacity = 200;
            
            // 总是解锁
            alwaysUnlocked = true;
            
            // 死亡和碰撞音效音量
            wreckSoundVolume = 0.8f;
            deathSoundVolume = 0.7f;
        }};
        ProtectedUnit = new ProtectedUnitType("proxima-boss") {{
            // 基础配置
            health = 50000f;              // 基础血量（会被保护系统接管）
            armor = 30f;                 // 护甲
            hitSize = 32f;               // 碰撞箱大小（约4格）


            // 移动参数
            flying = true;               // 飞行单位
            speed = 20f;               // 移动速度
            drag = 0.02f;               // 阻力
            accel = 0.05f;              // 加速度
            rotateSpeed = 3f;            // 旋转速度

            // 战斗参数
            targetAir = true;
            targetGround = true;
            range = 400f;                // 攻击范围

            // 视觉参数
            lowAltitude = false;
            engineSize = 3f;

            // 武器配置
            weapons.add(new Weapon("proxima-boss-cannon") {{
                x = 0f;
                y = 10f;
                reload = 60f;
                shootSound = Sounds.shootReign;
                bullet = new BasicBulletType(8f, 200) {{
                    lifetime = 60f;
                    damage = 50;
                    splashDamage = 100;
                    splashDamageRadius = 40f;
                    status = StatusEffects.burning;
                    statusDuration = 300f;
                    hitEffect = Fx.hitLancer;
                    shootEffect = Fx.shootBig;
                    smokeEffect = Fx.shootBigSmoke;
                }};
            }});

            // 总是解锁（用于测试）
            alwaysUnlocked = true;
        }};
        sprout = new UnitType("sprout") {{
            // 构造器 - 多足单位
            constructor = LegsUnit::create;

            // 基础配置
            isEnemy = true;
            flying = false;

            // 移动参数 - 植物系较慢速
            speed = 1.2f;
            hitSize = 10.4f;
            health = 180f;
            armor = 2f;
            drag = 0.4f;
            accel = 0.3f;
            rotateSpeed = 4f;

            // 多足配置 (4条腿，类似小型蜘蛛/植物根茎)
            legCount = 4;
            legGroupSize = 2;
            legLength = 7f;
            legSpeed = 0.12f;
            legForwardScl = 1.2f;
            legBaseOffset = 0f;
            legMoveSpace = 0.9f;
            legContinuousMove = true;

            // 视觉参数
            lowAltitude = true;
            engineSize = -1f;  // 无引擎

            // 战斗参数
            targetAir = false;   // 只能攻击地面
            targetGround = true;
            range = 120f;

            // 颜色参数 (植物系)
            lightColor = Color.valueOf("6aad49");

            // 液体恢复能力 - 在水中恢复生命
            abilities.add(new LiquidRegenAbility() {{
                liquid = Liquids.water;      // 使用水作为恢复液体
                slurpSpeed = 8f;             // 每秒吸取8单位液体
                regenPerSlurp = 1f;          // 每吸取1单位液体恢复1生命值
                slurpEffectChance = 0.5f;    // 50%概率触发治疗特效
                slurpEffect = Fx.heal;       // 治疗特效
            }});

            // 武器 - 绿色植物团弹
            weapons.add(new Weapon("sprout-cannon") {{
                x = 0f;
                y = 4f;
                reload = 35f;
                shootY = 2f;
                shootSound = Sounds.shoot;

                bullet = new BasicBulletType(4.5f, 28) {{
                    sprite = "proxima-asteroid-bullet";
                    width = 8f;
                    height = 8f;
                    lifetime = 50f;
                    damage = 12;
                    splashDamage = 12;
                    splashDamageRadius = 12f;
                    status = StatusEffects.corroded;
                    statusDuration = 60f;

                    // 绿色弹丸
                    backColor = frontColor = trailColor = Color.valueOf("6aad49");
                    hitEffect = shootEffect = smokeEffect = Fx.none;
                    hitEffect = Fx.none;

                    collidesAir = false;
                    collidesGround = true;
                    shrinkX = shrinkY = 0.5f;
                }};
            }});

            // 总是解锁（测试用）
            alwaysUnlocked = true;
        }};
    }
}
