package Proxima.block;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.Effect;
import mindustry.entities.TargetPriority;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.Block;

import static mindustry.Vars.*;

/**
 * DFC 激光发射器
 * 继承自 DFCBase，实现激光发射功能
 */
public class DFCLaserEmitter extends DFCBase {

    public float laserRange = 400f;
    public float laserDamage = 100f;
    public float laserChargeTime = 60f;
    public float laserDuration = 20f;
    public float laserWidth = 8f;
    public float rotationSpeed = 5f;
    
    // 更鲜艳的激光颜色
    public Color laserColor = Color.valueOf("FF4500"); // 鲜艳的橙红色
    public Color laserColor2 = Color.valueOf("FF1493"); // 鲜艳的粉红色
    
    public Effect chargeEffect = Fx.none;
    public Effect shootEffect = Fx.none;
    public Effect hitEffect = Fx.none;

    public DFCLaserEmitter(String name) {
        super(name);
        this.range = laserRange;
        this.rotateSpeed = rotationSpeed;
        this.activationTime = 120f;
        
        // 设置建筑大小为3
        size = 3;
        
        // 移除所有资源消耗需求
        outputsPower = false;
        consumesPower = false;
        hasItems = false;
        hasLiquids = false;
        
        // 启用配置功能
        configurable = true;
        saveConfig = false;
        
        coolantMultiplier = 2f;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.damage, laserDamage / laserDuration * 60f, StatUnit.perSecond);
        stats.add(Stat.range, laserRange / tilesize, StatUnit.blocks);
        stats.add(Stat.charge, laserChargeTime / 60f, StatUnit.seconds);
    }

    @Override
    void checkDrawDefault() {
        if(drawer == null) {
            // 不使用 DrawPower 渲染器，因为激光发射器不需要电力
            drawer = new mindustry.world.draw.DrawMulti(
                new mindustry.world.draw.DrawDefault(),
                new mindustry.world.draw.DrawRegion("-top")
            );
        }
    }

    public class DFCLaserEmitterBuild extends DFCBlockBuild {
        public float laserTime = 0f;
        public Building target;
        public Building dfcCoreTarget; // DFCore目标
        public float targetAngle;
        public boolean charging = false;

        @Override
        public void update() {
            super.update();
            
            if (activationTimer <= 0) {
                findTarget();
                
                // 即使没有目标也可以开火
                Building currentTarget = dfcCoreTarget != null ? dfcCoreTarget : target;
                if (currentTarget != null) {
                    // 旋转朝向目标
                    float angle = angleTo(currentTarget.x, currentTarget.y);
                    rotation = Mathf.slerp(rotation, angle, Time.delta * rotateSpeed / 180f);
                    targetAngle = angle;
                }
                
                // 持续发射逻辑
                laserTime = laserDuration; // 保持激光持续发射
                charging = false;
                
                // 持续造成伤害
                if (target != null && dfcCoreTarget == null) {
                    target.damage(laserDamage / laserDuration * Time.delta);
                }
            } else {
                laserTime = 0;
                charging = false;
            }
        }

        private void findTarget() {
            target = null;
            float bestDistance = Float.MAX_VALUE;
            
            // 优化的目标搜索：使用矩形区域搜索
            int radius = (int)(laserRange / tilesize) + 1;
            int tileX = tileX();
            int tileY = tileY();
            
            for (int i = tileX - radius; i <= tileX + radius; i++) {
                for (int j = tileY - radius; j <= tileY + radius; j++) {
                    if (i >= 0 && i < world.width() && j >= 0 && j < world.height()) {
                        Building building = world.build(i, j);
                        if (building != null && building.team != team) {
                            float distance = Mathf.dst(x, y, building.x, building.y);
                            if (distance <= laserRange && distance < bestDistance) {
                                bestDistance = distance;
                                target = building;
                            }
                        }
                    }
                }
            }
        }

        private void shoot() {
            if (target != null) {
                // 播放射击特效
                shootEffect.at(x, y, rotation, laserColor);
            }
        }

        @Override
        public void draw() {
            super.draw();
            
            // 暂停游戏时停止渲染激光
            if (state.isPaused()) {
                return;
            }
            
            // 绘制激光束
            if (laserTime > 0) {
                float alpha = Mathf.clamp(laserTime / laserDuration);
                
                // 计算1.5个游戏格前的发射点
                float rad = rotation * Mathf.degRad;
                float startX = x + Mathf.cos(rad) * 1.5f * tilesize;
                float startY = y + Mathf.sin(rad) * 1.5f * tilesize;
                
                Building currentTarget = dfcCoreTarget != null ? dfcCoreTarget : target;
                if (currentTarget != null) {
                    // 有目标时，激光指向目标
                    drawLaser(startX, startY, currentTarget.x, currentTarget.y, alpha);
                    
                    // 绘制命中特效
                    if (Mathf.chance(0.1)) {
                        hitEffect.at(currentTarget.x, currentTarget.y, 0, laserColor);
                    }
                } else {
                    // 无目标时，激光沿当前旋转方向发射
                    float endX = startX + Mathf.cos(rad) * laserRange;
                    float endY = startY + Mathf.sin(rad) * laserRange;
                    drawLaser(startX, startY, endX, endY, alpha);
                }
            }
            
            // 不再需要充电特效，因为激光持续发射

        }
        
        private void drawLaser(float startX, float startY, float endX, float endY, float alpha) {
            float length = Mathf.dst(startX, startY, endX, endY);
            int count = (int)(length / 12f) + 2;
            
            // NTM风格：基于长度的动态宽度，放大4倍
            float dynamicWidth = ((float)Math.max(1, Math.log10(length) - 3) / 8f * alpha) * 4f;
            if(dynamicWidth < 0.8f) dynamicWidth = 0.8f; // 最小宽度也放大4倍
            
            // 直接计算激光路径上的点，而不是使用旋转角度
            // 这样可以确保激光总是从起点指向终点
            
            // 绘制主激光（中部直线，不发生偏移）
            drawNTMLaserLayer(startX, startY, endX, endY, length, count, alpha, 
                laserColor, dynamicWidth * 2f, 0.8f, false, 0, 0);
            
            // 绘制第一层外部激光（正弦波偏移）
            drawNTMLaserLayer(startX, startY, endX, endY, length, count, alpha, 
                laserColor, dynamicWidth * 1.5f, 0.6f, true, 10f, 0);
            
            // 绘制第二层外部激光（正弦波偏移，不同相位）
            drawNTMLaserLayer(startX, startY, endX, endY, length, count, alpha, 
                laserColor2, dynamicWidth, 0.5f, true, 15f, 180f);
            
            // 给激光添加发光效果（NTM风格），发光范围也放大4倍
            Drawf.light(startX, startY, endX, endY, 200f * alpha, laserColor, 0.6f * alpha);
            
            Draw.reset();
        }
        
        /**
         * 绘制NTM风格的激光层
         */
        private void drawNTMLaserLayer(float startX, float startY, float endX, float endY, 
                                      float length, int count, float alpha, 
                                      Color color, float width, float opacity, boolean wave, 
                                      float waveFrequency, float waveOffset) {
            Draw.color(color.cpy().a(opacity * alpha));
            Lines.stroke(width);
            Lines.beginLine();
            
            for(int i = 0; i < count; i++){
                float fin = i / (count - 1f);
                
                // 直接计算直线上的点
                float x = Mathf.lerp(startX, endX, fin);
                float y = Mathf.lerp(startY, endY, fin);
                
                if(wave) {
                    // 计算垂直于激光方向的偏移
                    float dx = endX - startX;
                    float dy = endY - startY;
                    // 计算单位垂直向量
                    float perpX = -dy / length;
                    float perpY = dx / length;
                    
                    // 计算正弦波偏移
                    float waveAmplitude = 5f; // 增大正弦波幅度
                    float waveValue = Mathf.sin(fin * waveFrequency + Time.time / 10f + waveOffset) * waveAmplitude * (1f - fin);
                    
                    // 应用偏移
                    x += perpX * waveValue;
                    y += perpY * waveValue;
                }
                
                Lines.linePoint(x, y);
            }
            
            Lines.endLine(false);
        }
        


        @Override
        public void drawSelect() {
            super.drawSelect();
            
            // 绘制激光范围
            Drawf.dashCircle(x, y, laserRange, team.color);
        }

        @Override
        public float estimateDps() {
            return laserDamage / (laserChargeTime + laserDuration) * 60f;
        }
        
        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (this == other) {
                return false;
            }
            
            // 检查是否是DFCore
            if (other.block instanceof DFCore && within(other, laserRange) && other.team == team) {
                // 切换DFCore目标
                if (dfcCoreTarget == other) {
                    dfcCoreTarget = null;
                } else {
                    dfcCoreTarget = other;
                }
                return false;
            }
            
            return true;
        }
        
        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            // 绘制激光范围
            Drawf.dashCircle(x, y, laserRange, team.color);
            
            // 绘制与DFCore的连接
            if (dfcCoreTarget != null) {
                Drawf.square(dfcCoreTarget.x, dfcCoreTarget.y, dfcCoreTarget.block.size * tilesize / 2 + 1, team.color);
                Lines.stroke(1);
                Lines.dashLine(x, y, dfcCoreTarget.x, dfcCoreTarget.y, team.color.rgba());
            }
        }
        
        @Override
        public Object config() {
            return dfcCoreTarget != null ? dfcCoreTarget.pos() : -1;
        }
        
        @Override
        public void configure(Object value) {
            if (value instanceof Integer) {
                int pos = (Integer)value;
                if (pos == -1) {
                    dfcCoreTarget = null;
                } else {
                    Building build = world.build(pos);
                    if (build != null && build.block instanceof DFCore) {
                        dfcCoreTarget = build;
                    }
                }
            }
        }
        
        @Override
        public void write(arc.util.io.Writes write) {
            super.write(write);
            write.i(dfcCoreTarget != null ? dfcCoreTarget.pos() : -1);
        }
        
        @Override
        public void read(arc.util.io.Reads read, byte revision) {
            super.read(read, revision);
            int pos = read.i();
            if (pos != -1) {
                Building build = world.build(pos);
                if (build != null && build.block instanceof DFCore) {
                    dfcCoreTarget = build;
                }
            }
        }
    }
}