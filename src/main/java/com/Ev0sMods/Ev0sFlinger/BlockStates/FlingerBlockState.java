package com.Ev0sMods.Ev0sFlinger.BlockStates;





//import com.github.javaparser.quality.Nullable;
import com.Ev0sMods.Ev0sFlinger.Util.ItemUtilsExtended;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.inventory.container.filter.SlotFilter;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.state.TickableBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.codec.KeyedCodec;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;


import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("removal")
public class FlingerBlockState extends ItemContainerState implements TickableBlockState, ItemContainerBlockState {
    private float force;
    private List<Ref<EntityStore>> currentFlingable = new ArrayList<Ref<EntityStore>>();
    public static final BuilderCodec<FlingerBlockState> CODEC = BuilderCodec.builder(FlingerBlockState.class, FlingerBlockState::new, BlockState.BASE_CODEC).append(new KeyedCodec<>("Force", BuilderCodec.FLOAT, false), (o, v) -> o.force = v, o -> o.force).add().build();
    protected Data data;
    private int timer = 0;
    private int f = 0;

    private boolean isNotShowing = true;
    private List<Ref<EntityStore>> l = new ArrayList<>();
    private boolean flingable = true;
    public FlingerBlockState(){
        f = 0;
    }
    private ItemContainer ic;
    public boolean initialize(BlockType blockType) {
        if (super.initialize(blockType) && blockType.getState() instanceof Data data) {

            this.setItemContainer(new SimpleItemContainer((short) 1));
            this.itemContainer.setSlotFilter(FilterActionType.ADD, (short) 0, new SlotFilter() {
                @Override
                public boolean test(FilterActionType filterActionType, ItemContainer itemContainer, short i, @NullableDecl ItemStack itemStack) {
                    if(itemContainer.isEmpty() && itemStack.getQuantity()<2){
                        return true;
                    } else{return false;
                    }
                }
            });
            this.data = data;
            return true;
        }

        return false;

    }
    @Nonnull
    public static List<Ref<EntityStore>> getAllEntitiesInBox(FlingerBlockState hp, Vector3i pos, float height, @Nonnull ComponentAccessor<EntityStore> components) {
        final ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        final ObjectList<Ref<Store>> results2 = SpatialResource.getThreadLocalReferenceList();
        final Vector3d min = new Vector3d(pos.x-.1, pos.y , pos.z-.1);
        final Vector3d max = new Vector3d(pos.x+.1, pos.y, pos.z+.1);

            components.getResource(EntityModule.get().getPlayerSpatialResourceType()).getSpatialStructure().collectCylinder(new Vector3d(pos.x,pos.y,pos.z), 8, 160, results );
        //components.getResource(EntityModule.get().getItemSpatialResourceType()).getSpatialStructure().collectCylinder(new Vector3d(pos.x,pos.y,pos.z), 256, 256, results );
        //hp.ca = components;
        return results;
    }
    @Nonnull
    public static List<Ref<EntityStore>> getAllItemsInBox(FlingerBlockState hp, Vector3i pos, float height, @Nonnull ComponentAccessor<EntityStore> components) {
        final ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        final ObjectList<Ref<Store>> results2 = SpatialResource.getThreadLocalReferenceList();
        final Vector3d min = new Vector3d(pos.x-.1, pos.y , pos.z-.1);
        final Vector3d max = new Vector3d(pos.x+.1, pos.y, pos.z+.1);

        //components.getResource(EntityModule.get().getPlayerSpatialResourceType()).getSpatialStructure().collectCylinder(new Vector3d(pos.x,pos.y,pos.z), 8, 160, results );
        components.getResource(EntityModule.get().getItemSpatialResourceType()).getSpatialStructure().collectCylinder(new Vector3d(pos.x,pos.y,pos.z), 256, 256, results );
        //hp.ca = components;
        return results;
    }


    @Override
    public void tick(float v, int i, ArchetypeChunk<ChunkStore> archetypeChunk, Store<ChunkStore> store, CommandBuffer<ChunkStore> commandBuffer) {


            HytaleLogger.getLogger().atInfo().log(String.valueOf(this.getRotationIndex()));

            final World world = store.getExternalData().getWorld();
            final Store<EntityStore> entities = world.getEntityStore().getStore();
            ic = this.getItemContainer();
            if (ic != null) {
                if(timer>100) {
                    timer =0;


                isNotShowing = false;
                if (!ic.isEmpty()) {
                    ItemStack is = this.ic.getItemStack((short) 0);
                }


                for (Ref<EntityStore> target : getAllEntitiesInBox(this, this.getBlockPosition(), 0, entities)) {
                    if (!ic.isEmpty()) {
                        if (flingable) {

                            Ref<EntityStore> rf = ItemUtilsExtended.throwItem(target, new Vector3d(getBlockX(), getBlockY() + 1.5, getBlockZ()), (ComponentAccessor<EntityStore>) entities, this.getItemContainer().getItemStack((short) 0).withQuantity(1), Vector3d.ZERO, 0, 20);
                            currentFlingable.add(rf);
                            flingable = false;
                        }

                    }
                    for (Ref<EntityStore> target2 : getAllItemsInBox(this, this.getBlockPosition(), 0, entities)) {
                        if (!ic.isEmpty()) {
                            if (flingable) {


                                Ref<EntityStore> rf = ItemUtilsExtended.throwItem(target2, new Vector3d(getBlockX(), getBlockY() + 1, getBlockZ()), (ComponentAccessor<EntityStore>) entities, this.getItemContainer().getItemStack((short) 0).withQuantity(1), Vector3d.ZERO, 0, 0);
                                currentFlingable.add(rf);
                                flingable = false;
                            }

                        }


                        for (int t = 0; t < currentFlingable.size() - 1; t++) {
                            if (!currentFlingable.isEmpty()) {
                                Ref<EntityStore> iex = currentFlingable.get(t);
                                if (iex != null) {
                                    PhysicsValues pv = new PhysicsValues(1, 0, false);
                                    ((PhysicsValues) iex.getStore().ensureAndGetComponent(target2, PhysicsValues.getComponentType())).replaceValues(pv);

                                    //iex.ensureAndGetComponent(TransformComponent.getComponentType()).setPosition(new Vector3d(getBlockX(), getBlockY() +20, getBlockZ()));
                                    //((Velocity) iex.ensureAndGetComponent(Velocity.getComponentType())).addForce(0, force, 0);
                                }

                            }

                        }
                    }
                }
                if (!flingable) {
                    this.ic.removeItemStackFromSlot((short) 0, 1);

                }
            }else{timer++;}
                f++;
                Vector3d pos = this.getBlockPosition().toVector3d().add(0, 3, 0);
                if (!currentFlingable.isEmpty()) {
                    Ref<EntityStore> iex = currentFlingable.get(0);
                    for (Ref<EntityStore> target2 : getAllItemsInBox(this, this.getBlockPosition(), 0, entities)) {
                        iex = target2;

                        HytaleLogger.getLogger().atInfo().log("Tick show and move2" + ", " + f);


                        if (f <= 4) {
                            flingable = false;

                            if (iex.isValid()) {
                                if (this.getRotationIndex() == 0) {
                                    iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos.x, pos.y + .1, pos.z - .1));
                                }
                                if (this.getRotationIndex() == 2) {
                                    iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos.x, pos.y + .1, pos.z + .1));
                                }
                                if (this.getRotationIndex() == 1) {
                                    iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos.x - .1, pos.y + .1, pos.z));
                                }
                                if (this.getRotationIndex() == 3) {
                                    iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos.x + .1, pos.y + .1, pos.z));
                                }
                                PhysicsValues pv = new PhysicsValues(1, 0, false);
                                ((PhysicsValues) iex.getStore().ensureAndGetComponent(iex, PhysicsValues.getComponentType())).replaceValues(pv);
                                HytaleLogger.getLogger().atInfo().log(pos + "X");
                                //iex.ensureAndGetComponent(Velocity.getComponentType()).addForce(0, force, 0);
                                //updatePos(iex, holder);
                                HytaleLogger.getLogger().atInfo().log(String.valueOf(f));

                            }
                        }
                        Vector3d pos3 = iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).getPosition();
                        if (pos3.y <= this.getBlockY()) {
                            HytaleLogger.getLogger().atInfo().log("Removing Item");
                            flingable = true;
                            iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos3.x, pos3.y, pos3.z));
                            f = 0;

                            ItemComponent itemComponent = iex.getStore().ensureAndGetComponent(iex, ItemComponent.getComponentType());
                            itemComponent.setPickupDelay(0);
                            itemComponent.setRemovedByPlayerPickup(true);
                            assert getChunk() != null;
                            ItemStack isi = itemComponent.getItemStack();
                            if (world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x), (int) pos3.y, (int) pos3.z) instanceof ItemContainerState) {
                                ItemStackTransaction ist = ((ItemContainerState) world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x + 1), (int) pos3.y, (int) pos3.z)).getItemContainer().addItemStack(isi);
                                if (ist.succeeded()) {
                                    iex.getStore().removeEntity(currentFlingable.getFirst(), RemoveReason.REMOVE);
                                    currentFlingable.removeFirst();
                                    flingable = true;

                                }
                            } else if (world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x) + 1, (int) pos3.y, (int) pos3.z) instanceof ItemContainerState) {
                                ItemStackTransaction ist = ((ItemContainerState) world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x + 1, pos3.z)).getState((int) (pos3.x + 1), (int) pos3.y, (int) pos3.z)).getItemContainer().addItemStack(isi);
                                if (ist.succeeded()) {
                                    iex.getStore().removeEntity(currentFlingable.getFirst(), RemoveReason.REMOVE);
                                    currentFlingable.removeFirst();
                                    flingable = true;

                                }
                            } else if (world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x - 1), (int) pos3.y, (int) pos3.z) instanceof ItemContainerState) {
                                ItemStackTransaction ist = ((ItemContainerState) world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x - 1), (int) pos3.y, (int) pos3.z)).getItemContainer().addItemStack(isi);
                                if (ist.succeeded()) {
                                    iex.getStore().removeEntity(currentFlingable.getFirst(), RemoveReason.REMOVE);
                                    currentFlingable.removeFirst();
                                    flingable = true;
                                }
                            } else if (world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x), (int) pos3.y, (int) pos3.z - 1) instanceof ItemContainerState) {
                                ItemStackTransaction ist = ((ItemContainerState) world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x), (int) pos3.y, (int) pos3.z - 1)).getItemContainer().addItemStack(isi);
                                if (ist.succeeded()) {
                                    iex.getStore().removeEntity(currentFlingable.getFirst(), RemoveReason.REMOVE);
                                    currentFlingable.removeFirst();
                                    flingable = true;
                                }
                            } else if (world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x), (int) pos3.y, (int) pos3.z + 1) instanceof ItemContainerState) {
                                ItemStackTransaction ist = ((ItemContainerState) world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos3.x, pos3.z)).getState((int) (pos3.x), (int) pos3.y, (int) pos3.z + 1)).getItemContainer().addItemStack(isi);
                                if (ist.succeeded()) {
                                    iex.getStore().removeEntity(currentFlingable.getFirst(), RemoveReason.REMOVE);
                                    currentFlingable.removeFirst();
                                    flingable = true;
                                }
                            }
//                                if (getChunk().getState(0, 1, 0) instanceof ItemContainerState) {
//                                    ItemStackTransaction ist = ((ItemContainerState) getChunk().getState(0, 1, 0)).getItemContainer().addItemStack(isi);
//                                    if (ist.succeeded()) {
//                                        currentFlingable.removeFirst();
//                                    }
//                                }
//                                if (getChunk().getState(0, -1, 0) instanceof ItemContainerState) {
//                                    ItemStackTransaction ist = ((ItemContainerState) getChunk().getState(0, -1, 0)).getItemContainer().addItemStack(isi);
//                                    if (ist.succeeded()) {
//                                        currentFlingable.removeFirst();
//                                    }
//                                }
                            else {
                                if (!currentFlingable.isEmpty()) {
                                    currentFlingable.removeFirst();
                                    if (currentFlingable.getFirst() != null) {
                                        return;
                                    } else {
                                        flingable = true;
                                    }

                                } else {
                                    flingable = true;
                                }
                            }

                        }
                    }


                    if (f >= 5) {
                        f = 5;
                        if (iex.isValid()) {
                            Vector3d pos2 = iex.getStore().getComponent(iex, TransformComponent.getComponentType()).getPosition();
                            if (getRotationIndex() == 0) {
                                iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos2.x, pos2.y - .5, pos2.z - .1));
                            }
                            if (getRotationIndex() == 2) {
                                iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos2.x, pos2.y - .5, pos2.z + .1));
                            }
                            if (getRotationIndex() == 1) {
                                iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos2.x - .1, pos2.y - .5, pos2.z));
                            }
                            if (getRotationIndex() == 3) {
                                iex.getStore().ensureAndGetComponent(iex, TransformComponent.getComponentType()).setPosition(new Vector3d(pos2.x + .1, pos2.y - .5, pos2.z));
                            }
                            //iex.getStore().removeComponent(iex, TransformComponent.getComponentType());
                            if (iex.isValid()) {
                                //iex.getStore().removeEntity(iex, RemoveReason.REMOVE);

                                if (iex.getStore().getComponent(iex, PlayerRef.getComponentType()) != null) {
                                    if (!iex.getStore().getComponent(iex, PlayerRef.getComponentType()).isValid()) {


                                        //iex = null;
                                        //currentFlingable.removeFirst();
                                        flingable = true;
                                    }
                                    if (iex != null) {
                                        iex.getStore().removeEntity(iex, RemoveReason.REMOVE);
                                    }
                                }


                            }
                        }
                    }
                }


            }









    }
    public static ComponentType<EntityStore, BlockEntity> getComponentType() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = EntityModule.get().getEntityStoreRegistry();
        return EntityModule.get().getBlockEntityComponentType();
    }


    public static class Data extends StateData{
        @Nonnull
        public static final BuilderCodec<Data> CODEC = BuilderCodec.<Data>builder(Data.class, Data::new, StateData.DEFAULT_CODEC).append(new KeyedCodec<>("Force", BuilderCodec.FLOAT), (o,v) -> o.force = v, o -> o.force).add().build();

        public float force;
    }
}

