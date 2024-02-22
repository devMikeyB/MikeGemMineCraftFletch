package net.botwithus;

import com.sun.jdi.InterfaceType;
import net.botwithus.api.game.hud.Dialog;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.BackpackInventory;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Inventory;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.minimenu.actions.MiniMenuAction;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GemMiner extends LoopingScript {

    private BotState botState = BotState.MINING;
    private boolean someBool = true;
    private Random random = new Random();
    private ArrayList<String> droppableItems = new ArrayList<>();
    private int playerLevel;

    enum GemRocks {
        COMMON_ROCK("Common gem rock"),
        UNCOMMON_ROCK("Uncommon gem rock");
        GemRocks(String name) {
            this.name = name;
        }
        private final String name;
        public String getName() {
            return name;
        }
    }

    enum UncutGems {
        OPAL("Uncut opal", 1),
        LAPIZ_LAZULI("Uncut lapis lazuli", 1),
        JADE("Uncut jade", 13),
        RED_TOPAZ("Uncut red topaz", 16),
        SAPPHIRE("Uncut sapphire", 20),
        EMERALD("Uncut emerald", 27),
        RUBY("Uncut ruby", 34),
        CRUSHED_GEM("Crushed gem", 999);

        UncutGems(String name, int i) {
            this.name = name;
            this.level = i;
        }
        private final String name;
        private final int level;
        public String getName() {
            return name;
        }
        public int getLevel() {
            return level;
        }
    }

    enum Gems {
        OPAL("Opal", 11),
        JADE("Jade", 26),
        RED_TOPAZ("Red topaz", 48),
        SAPPHIRE("Sapphire", 56),
        EMERALD("Emerald", 58),
        RUBY("Ruby", 63),
        LAPIZ_LAZULI("Lapis lazuli gem", 999);
        Gems(String name, int i) {
            this.name = name;
            this.level = i;
        }
        private final String name;
        private final int level;
        public String getName() {
            return name;
        }
        public int getLevel() {
            return level;
        }
    }

    enum BotState {
        //define your own states here
        IDLE,
        MINING,
        CUTTING,
        FLETCHING,
        DROPPING,
        //...
    }

    public GemMiner(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);
    }

    @Override
    public void onLoop() {
        //Loops every 100ms by default, to change:
        //this.loopDelay = 500;
        LocalPlayer player = Client.getLocalPlayer();
        buildDroppableItems();


        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN || botState == BotState.IDLE) {
            //wait some time so we dont immediately start on login.
            Execution.delay(random.nextLong(3000,7000));
            return;
        }
        switch (botState) {
            case IDLE -> {
                //do nothing
                println("We're idle!");
                Execution.delay(random.nextLong(1000,3000));
            }
            case MINING -> {
                Execution.delay(handleMining(player));
            }
            case CUTTING -> {
                //do some code that handles your cutting.
                try {
                    Execution.delay(handleCutting(player));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void buildDroppableItems() {
        //Clear the list
        droppableItems.clear();

        //Add items to the list
        for (UncutGems gem : UncutGems.values()) {
            if (Skills.CRAFTING.getActualLevel() < gem.getLevel()) {
                droppableItems.add(gem.getName());
            }
        }
        for (Gems gem : Gems.values()) {
            if (Skills.FLETCHING.getLevel() < gem.getLevel()) {
                println(gem.getName() + ": " + gem.getLevel() + " > ? " + Skills.FLETCHING.getActualLevel());
                droppableItems.add(gem.getName());
            }
        }
    }

    private long handleCutting(LocalPlayer player) throws InterruptedException {
        Execution.delay(craftInventoryItems());
        //Check to make sure we aren't still crafting before dropping.
        if (Interfaces.isOpen(1251)) {
            try {
                AwaitCondition.await(() -> !Interfaces.isOpen(1251), 15, TimeUnit.SECONDS);
                return random.nextLong(250,1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        //Drop that shit.
        for (Item item : Backpack.getItems()){
            if (droppableItems.contains(item.getName())){
                if (Backpack.interact(item.getName(), "Drop")) {
                    Execution.delay(random.nextLong(250,1500));
                }
            }
        }
        botState = BotState.MINING;
        return random.nextLong(250,1500);
    }

    private long craftInventoryItems() throws InterruptedException {
        for (Item item : Backpack.getItemsWithOption("Craft")){
            if (Interfaces.isOpen(1251)) {
                AwaitCondition.await(() -> !Interfaces.isOpen(1251), 15, TimeUnit.SECONDS);
                return random.nextLong(250,1500);
            }
            if (!droppableItems.contains(item.getName())) {
                println(String.valueOf(item.getName()));
                if(Backpack.interact(item.getName(), "Craft")){
                    Execution.delayUntil(3000, () -> Interfaces.isOpen(1371));
                    if(MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350)){
                        Execution.delayUntil(3000, () -> !Interfaces.isOpen(1371));
                        Execution.delayUntil(3000, () -> !Interfaces.isOpen(1251));
                    }
                }
                //return random.nextLong(250,1500);
            } else if (Backpack.interact(item.getName(), "Drop")) {
                Execution.delay(random.nextLong(250,1500));
            }
        }
        return random.nextLong(250,1500);
    }

    private long handleMining(LocalPlayer player) {
        //if our inventory is full, lets bank.
        if (Backpack.isFull()) {
            botState = BotState.CUTTING;
            return random.nextLong(250,1500);
        }
        //click my tree, mine my rock, etc...
        SceneObject gemRock = SceneObjectQuery.newQuery().name("Common gem rock").option("Mine").results().nearest();
        if (gemRock != null) {
            println("Interacted rock: " + gemRock.interact("Mine"));
        }
        return random.nextLong(1500,3000);
    }

    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public boolean isSomeBool() {
        return someBool;
    }

    public void setSomeBool(boolean someBool) {
        this.someBool = someBool;
    }
}
