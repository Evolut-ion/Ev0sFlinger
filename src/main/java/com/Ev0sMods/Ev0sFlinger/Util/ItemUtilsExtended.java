package com.Ev0sMods.Ev0sFlinger.Util;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.iterator.CircleIterator;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSettings;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static com.hypixel.hytale.server.core.modules.entity.item.ItemComponent.generateItemDrop;

public class ItemUtilsExtended {
    @Nonnull
    public static ComponentType<EntityStore, ItemComponent> getComponentItemType() {
        return EntityModule.get().getItemComponentType();
    }

    public static void interactivelyPickupItem(@Nonnull Ref<EntityStore> ref, @Nonnull ItemStack itemStack, @Nullable Vector3d origin, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        LivingEntity entity = (LivingEntity) EntityUtils.getEntity(ref, componentAccessor);
        InteractivelyPickupItemEvent event = new InteractivelyPickupItemEvent(itemStack);
        componentAccessor.invoke(ref, event);
        if (event.isCancelled()) {
            dropItem(ref, itemStack, componentAccessor);
        } else {
            Player playerComponent = (Player)componentAccessor.getComponent(ref, Player.getComponentType());
            if (playerComponent != null) {
                TransformComponent transformComponent = (TransformComponent)componentAccessor.getComponent(ref, TransformComponent.getComponentType());

                assert transformComponent != null;

                PlayerSettings playerSettingsComponent = (PlayerSettings)componentAccessor.getComponent(ref, PlayerSettings.getComponentType());
                if (playerSettingsComponent == null) {
                    playerSettingsComponent = PlayerSettings.defaults();
                }

                Holder<EntityStore> pickupItemHolder = null;
                Item item = itemStack.getItem();
                ItemContainer itemContainer = playerComponent.getInventory().getContainerForItemPickup(item, playerSettingsComponent);
                ItemStackTransaction transaction = itemContainer.addItemStack(itemStack);
                ItemStack remainder = transaction.getRemainder();
                if (remainder != null && !remainder.isEmpty()) {
                    int quantity = itemStack.getQuantity() - remainder.getQuantity();
                    if (quantity > 0) {
                        ItemStack itemStackClone = itemStack.withQuantity(quantity);
                        playerComponent.notifyPickupItem(ref, itemStackClone, (Vector3d)null, componentAccessor);
                        if (origin != null) {
                            pickupItemHolder = ItemComponent.generatePickedUpItem(itemStackClone, origin, componentAccessor, ref);
                        }
                    }

                    dropItem(ref, remainder, componentAccessor);
                } else {
                    playerComponent.notifyPickupItem(ref, itemStack, (Vector3d)null, componentAccessor);
                    if (origin != null) {
                        pickupItemHolder = ItemComponent.generatePickedUpItem(itemStack, origin, componentAccessor, ref);
                    }
                }

                if (pickupItemHolder != null) {
                    componentAccessor.addEntity(pickupItemHolder, AddReason.SPAWN);
                }
            } else {
                SimpleItemContainer.addOrDropItemStack(componentAccessor, ref, entity.getInventory().getCombinedHotbarFirst(), itemStack);
            }

        }
    }

    @Nullable
    public static Ref<EntityStore> throwItem(@Nonnull Ref<EntityStore> ref, @Nonnull ItemStack itemStack, float throwSpeed, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        DropItemEvent.Drop event = new DropItemEvent.Drop(itemStack, throwSpeed);
        componentAccessor.invoke(ref, event);
        if (event.isCancelled()) {
            return null;
        } else {
            throwSpeed = event.getThrowSpeed();
            itemStack = event.getItemStack();
            if (!itemStack.isEmpty() && itemStack.isValid()) {
                HeadRotation headRotationComponent = (HeadRotation)componentAccessor.getComponent(ref, HeadRotation.getComponentType());

                assert headRotationComponent != null;

                Vector3f rotation = headRotationComponent.getRotation();
                Vector3d direction = Transform.getDirection(rotation.getPitch(), rotation.getYaw());
                return throwItem(ref, componentAccessor, itemStack, direction, throwSpeed);
            } else {
                HytaleLogger.getLogger().at(Level.WARNING).log("Attempted to throw invalid item %s at %s by %s", itemStack, throwSpeed, ref.getIndex());
                return null;
            }
        }
    }

    @Nullable
    public static Ref<EntityStore> throwItem(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> store, @Nonnull ItemStack itemStack, @Nonnull Vector3d throwDirection, float throwSpeed) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

        assert transformComponent != null;

        //assert modelComponent != null;

        Vector3d throwPosition = transformComponent.getPosition().clone();
        //Model model = modelComponent.getModel();
        //throwPosition.add((double)0.0F, .5f, (double)0.0F).add(throwDirection);
        Holder<EntityStore> itemEntityHolder = generateItemDrop(store, itemStack, throwPosition, Vector3f.ZERO, 0, -.1f, 0);
        if (itemEntityHolder == null) {
            return null;
        } else {
            ItemComponent itemComponent = (ItemComponent)itemEntityHolder.getComponent(ItemComponent.getComponentType());
            if (itemComponent != null) {
                itemComponent.setPickupDelay(20000000000f);
                itemComponent.setRemovedByPlayerPickup(false);
            }

            return store.addEntity(itemEntityHolder, AddReason.SPAWN);
        }
    }

    @Nullable
    public static Ref<EntityStore> throwItem(Vector3d pos, @Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> store, @Nonnull ItemStack itemStack, @Nonnull Vector3d throwDirection, float throwSpeed) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

        assert transformComponent != null;

        //assert modelComponent != null;

        Vector3d throwPosition = transformComponent.getPosition().clone();
        //Model model = modelComponent.getModel();
        //throwPosition.add((double)0.0F, .5f, (double)0.0F).add(throwDirection);
        Holder<EntityStore> itemEntityHolder = generateItemDrop(store, itemStack, new Vector3d(pos.x+.5,pos.y +1.5,pos.z+.5), Vector3f.ZERO, 0, -1, 0);
        if (itemEntityHolder == null) {
            return null;
        } else {
            ItemComponent itemComponent = (ItemComponent)itemEntityHolder.getComponent(ItemComponent.getComponentType());
            if (itemComponent != null) {

                itemComponent.setPickupDelay(1400000);
                itemComponent.setRemovedByPlayerPickup(false);
                itemComponent.computeDynamicLight();
                PhysicsValues pv = new PhysicsValues(0,0,true);

                ((PhysicsValues)itemEntityHolder.ensureAndGetComponent(PhysicsValues.getComponentType())).replaceValues(pv);
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(-1, 0, 0);
            }


            return store.addEntity(itemEntityHolder, AddReason.SPAWN);
        }
    }
    @Nullable
    public static Ref<EntityStore> throwItem(String side, Vector3d pos, @Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> store, @Nonnull ItemStack itemStack, @Nonnull Vector3d throwDirection, float throwSpeed) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

        assert transformComponent != null;

        //assert modelComponent != null;

        //Vector3d throwPosition = transformComponent.getPosition().clone();
        //Model model = modelComponent.getModel();
        //throwPosition.add((double)0.0F, .5f, (double)0.0F).add(throwDirection);
        Holder<EntityStore> itemEntityHolder = generateItemDrop(store, itemStack, new Vector3d(pos.x+.5,pos.y,pos.z+.5), Vector3f.ZERO, 0, -1, 0);
        if (itemEntityHolder == null) {
            return null;
        } else {
            ItemComponent itemComponent = (ItemComponent)itemEntityHolder.getComponent(ItemComponent.getComponentType());
            if (itemComponent != null) {
                final Instant currentTime = store.getResource(WorldTimeResource.getResourceType()).getGameTime();
                Instant despawnTime = Instant.ofEpochSecond((currentTime.toEpochMilli() + 1000));
                DespawnComponent d = new DespawnComponent();
                d.setDespawn(despawnTime);
                itemEntityHolder.ensureAndGetComponent(DespawnComponent.getComponentType()).setDespawn(despawnTime);
                itemComponent.setPickupDelay(100000000);

                itemComponent.setRemovedByPlayerPickup(false);
                itemComponent.computeDynamicLight();
                PhysicsValues pv = new PhysicsValues(0,0,true);

                ((PhysicsValues)itemEntityHolder.ensureAndGetComponent(PhysicsValues.getComponentType())).replaceValues(pv);
                itemEntityHolder.removeComponent(PhysicsValues.getComponentType());


                HytaleLogger.getLogger().atInfo().log(side);

            }
            if(side =="Up"){
                (itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, -1, 0);
            }
            if(side== "Down"){
                (itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 1, 0);
            }
            if(side =="North"){
                (itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 0, 1);
            }
            if(side== "South"){
                (itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 0, -1);
            }
            if(side =="East"){
                (itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(-1, 0, 0);
            }
            if(side == "West"){
                (itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(1, 0, 0);
            } else if(side == ""){
                (itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 1, 0);
            }

            return store.addEntity(itemEntityHolder, AddReason.SPAWN);
        }
    }

    public static Ref<EntityStore> throwItem(String blockId, String side, Vector3d pos, @Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> store, @Nonnull ItemStack itemStack, @Nonnull Vector3d throwDirection, float throwSpeed) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

        assert transformComponent != null;

        //assert modelComponent != null;

        Vector3d throwPosition = transformComponent.getPosition().clone();
        throwPosition.add((double)0.5F, .25, (double).5F);

        Holder<EntityStore> itemEntityHolder = generateItemDrop(store, itemStack, new Vector3d(pos.x+.5,pos.y+.2,pos.z+.5), Vector3f.ZERO, 0, -1, 0);
        if (itemEntityHolder == null) {
            return null;
        } else {

            ItemComponent itemComponent = (ItemComponent)itemEntityHolder.getComponent(ItemComponent.getComponentType());
            if (itemComponent != null) {
                itemEntityHolder.ensureAndGetComponent(EntityScaleComponent.getComponentType()).setScale(.5f);
                itemComponent.setPickupDelay(100000000);
                itemComponent.setRemovedByPlayerPickup(false);
                itemComponent.computeDynamicLight();
                PhysicsValues pv = new PhysicsValues(0,0,true);
                ((PhysicsValues)itemEntityHolder.ensureAndGetComponent(PhysicsValues.getComponentType())).replaceValues(pv);
                itemEntityHolder.removeComponent(PhysicsValues.getComponentType());
                //itemEntityHolder.tryRemoveComponent(Compon)

                //HytaleLogger.getLogger().atInfo().log(side);
                //HytaleLogger.getLogger().atInfo().log(blockId);

            }
            if(side =="Up"){
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, -1, 0);
            }
            if(side== "Down"){
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 1, 0);
            }
            if(side =="North"){
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 0, 1);
            }
            if(side== "South"){
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 0, -1);
            }
            if(side =="East"){
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(-1, 0, 0);
            }
            if(side == "West"){
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(1, 0, 0);
            } else if(side == ""){
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 1, 0);
            }

            return store.addEntity(itemEntityHolder, AddReason.SPAWN);
        }

    }
    public static Ref<EntityStore> throwItem( Ref<EntityStore> ref, Vector3d pos,  @Nonnull ComponentAccessor<EntityStore> store, @Nonnull ItemStack itemStack, @Nonnull Vector3d throwDirection, float speed, float force) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

        //assert transformComponent != null;

        //assert modelComponent != null;

        Vector3d throwPosition = transformComponent.getPosition().clone();
        throwPosition.add((double)0.5F, force, (double).5F);

        Holder<EntityStore> itemEntityHolder = generateItemDrop(store, itemStack, new Vector3d(pos.x+.5,pos.y+1.5,pos.z+.5), Vector3f.ZERO, 0, 60, 0);
        if (itemEntityHolder == null) {
            return null;
        } else {

            ItemComponent itemComponent = (ItemComponent)itemEntityHolder.getComponent(ItemComponent.getComponentType());
            if (itemComponent != null) {
                itemEntityHolder.ensureAndGetComponent(EntityScaleComponent.getComponentType()).setScale(.5f);
                itemComponent.setPickupDelay(5);
                itemComponent.setRemovedByPlayerPickup(false);
                itemComponent.computeDynamicLight();
                PhysicsValues pv = new PhysicsValues(1,0,false);
                ((PhysicsValues)itemEntityHolder.ensureAndGetComponent(PhysicsValues.getComponentType())).replaceValues(pv);
                //itemEntityHolder.removeComponent(PhysicsValues.getComponentType());
                //itemEntityHolder.tryRemoveComponent(Compon)

                //HytaleLogger.getLogger().atInfo().log(side);
                //HytaleLogger.getLogger().atInfo().log(blockId);

            }

            //((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).addForce(0, force, 0);


            return store.addEntity(itemEntityHolder, AddReason.SPAWN);
        }

    }
    @Nullable
    public static Holder<EntityStore> generateItemDrop(@Nonnull ComponentAccessor<EntityStore> accessor, @Nullable ItemStack itemStack, @Nonnull Vector3d position, @Nonnull Vector3f rotation, float velocityX, float velocityY, float velocityZ) {
        if (itemStack != null && !itemStack.isEmpty() && itemStack.isValid()) {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            ItemComponent itemComponent = new ItemComponent(itemStack);
            holder.addComponent(ItemComponent.getComponentType(), itemComponent);
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position.add(0,20,0), rotation));
           // ((Velocity)holder.ensureAndGetComponent(Velocity.getComponentType())).set((double)velocityX, (double)-35, (double)velocityZ);
            holder.ensureComponent(PhysicsValues.getComponentType());
            holder.ensureComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Intangible.getComponentType());
            float tempTtl = itemComponent.computeLifetimeSeconds(accessor);
            TimeResource timeResource = (TimeResource)accessor.getResource(TimeResource.getResourceType());
            holder.addComponent(DespawnComponent.getComponentType(), DespawnComponent.despawnInSeconds(timeResource, tempTtl));
            return holder;
        } else {
            HytaleLogger.getLogger().atInfo().log("Attempted to drop invalid item %s at %s", itemStack, position);
            return null;
        }
    }
    public static Ref<EntityStore> throwItem( @Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> store, @Nonnull ItemStack itemStack, @Nonnull Vector3d throwDirection, float throwSpeed, Vector3d pos) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

        assert transformComponent != null;

        //assert modelComponent != null;

        Vector3d throwPosition = transformComponent.getPosition().clone();
        //Model model = modelComponent.getModel();
        throwPosition.add((double)0.0F, 0.0f, (double).5F);
        transformComponent.setPosition(new Vector3d(pos.x, pos.y-.5, pos.z));
        Holder<EntityStore> itemEntityHolder = generateItemDrop(store, itemStack, pos, Vector3f.ZERO, 0, -.1f * throwSpeed, 0);
        if (itemEntityHolder == null) {
            return null;
        } else {
            ItemComponent itemComponent = (ItemComponent)itemEntityHolder.getComponent(ItemComponent.getComponentType());
            if (itemComponent != null) {
                itemComponent.setRemovedByPlayerPickup(false);
                itemComponent.setPickupDelay(2f);
                ((Velocity)itemEntityHolder.ensureAndGetComponent(Velocity.getComponentType())).set(0, 0, 0);


                TransformComponent tn = new TransformComponent(pos, Vector3f.ZERO);

            }

            return store.addEntity(itemEntityHolder, AddReason.SPAWN);
        }
    }


    @Nullable
    public static Ref<EntityStore> dropItem(@Nonnull Ref<EntityStore> ref, @Nonnull ItemStack itemStack, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        return throwItem(ref, itemStack, 1.0F, componentAccessor);
    }
}

