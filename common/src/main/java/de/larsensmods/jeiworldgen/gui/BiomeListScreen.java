package de.larsensmods.jeiworldgen.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class BiomeListScreen extends Screen {

    private final int paddedLineHeight = this.font.lineHeight + 6;

    private final Screen parent;

    private final Map<String, List<String>> namespaceSplitBiomes = new HashMap<>();
    private final List<String> biomeNamespaces;

    private boolean closed = false;
    private Button nextPageButton = null, previousPageButton = null, closeButton = null;
    private EditBox searchBox = null;

    private String searchTerm = "";
    private int page = 0;
    private int pageCount = 0;
    private int renderLinesPerPage = 0;
    private List<Component> renderLines;

    public BiomeListScreen(List<Identifier> biomes, Screen parent){
        super(Component.empty());
        this.parent = parent;

        for(Identifier biome : biomes){
            if(!namespaceSplitBiomes.containsKey(biome.getNamespace())){
                namespaceSplitBiomes.put(biome.getNamespace(), new ArrayList<>());
            }
            namespaceSplitBiomes.get(biome.getNamespace()).add(biome.getPath());
        }

        this.biomeNamespaces = namespaceSplitBiomes.keySet().stream().sorted((first, second) -> {
            if(first.equals(second)){
                return 0;
            }else if(first.equals("minecraft")){
                return -1;
            }else if(second.equals("minecraft")){
                return 1;
            }
            return first.compareTo(second);
        }).toList();

        for(String namespace : this.biomeNamespaces){
            this.namespaceSplitBiomes.get(namespace).sort(String::compareTo);
        }
    }

    @Override
    protected void init() {
        if(this.closed){
            this.minecraft.gui.setScreen(null);
            return;
        }
        closeButton = Button.builder(Component.translatable("gui.cancel"), _ -> this.onClose()).bounds(this.width / 2 - 60, this.height - 16 - 20, 120, 20).build();

        nextPageButton = Button.builder(Component.literal(">>"), _ -> {
            page++;
            if(page + 1 >= pageCount){
                nextPageButton.active = false;
            }
            previousPageButton.active = true;
        }).bounds(this.width / 2 + 38, 16, 20, 20).build();

        previousPageButton = Button.builder(Component.literal("<<"), _ -> {
            page--;
            if(page <= 0){
                previousPageButton.active = false;
            }
            nextPageButton.active = true;
        }).bounds(this.width / 2 - 58, 16, 20, 20).build();

        previousPageButton.active = false;
        if(pageCount <= 1){
            nextPageButton.active = false;
        }

        searchBox = new EditBox(this.font, this.width / 2 - 58, 40, 116, 20, Component.translatable("jeiwg.biome_search"));
        searchBox.setResponder(newText -> this.searchTerm = newText);

        this.addRenderableWidget(closeButton);
        this.addRenderableWidget(nextPageButton);
        this.addRenderableWidget(previousPageButton);
        this.addRenderableWidget(searchBox);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();

        int biomeTextRenderBaseY = 32 + nextPageButton.getHeight() + searchBox.getHeight() + 4;
        int biomeTextRenderHeight = guiHeight - 32 - closeButton.getHeight() - biomeTextRenderBaseY;

        recalcRenderLines(biomeTextRenderHeight);

        graphics.centeredText(this.font, "Page " + (page + 1) + "/" + pageCount, guiWidth / 2, 16 + 10 - (this.font.lineHeight / 2), 0xFFFFFFFF);
        previousPageButton.setPosition(guiWidth / 2 - 58, 16);
        nextPageButton.setPosition(guiWidth / 2 + 38, 16);
        searchBox.setPosition(guiWidth / 2 -58, 40);
        closeButton.setPosition(guiWidth / 2 - closeButton.getWidth() / 2, guiHeight - 16 - closeButton.getHeight());

        int startLine = Math.max(this.renderLinesPerPage * this.page, 0);
        int endLine = Math.clamp((long) this.renderLinesPerPage * (this.page + 1), startLine, this.renderLines.size());

        int entry = 0;
        for(int i = startLine; i < endLine; i++){
            Component line = this.renderLines.get(i);
            if(line.getStyle().isBold()) {
                graphics.centeredText(this.font, line, guiWidth / 2, biomeTextRenderBaseY + entry * this.paddedLineHeight, 0xFFFFFFFF);
            }else{
                graphics.text(this.font, Component.literal("-").append(line), guiWidth / 2 - 48, biomeTextRenderBaseY + entry * this.paddedLineHeight, 0xFFFFFFFF);
            }
            entry++;
        }
    }

    private void recalcRenderLines(int renderAreaHeight){
        this.renderLinesPerPage = renderAreaHeight / this.paddedLineHeight;

        String[] searchTerms = searchTerm.split(" ");
        String namespaceFilter = null;
        StringBuilder biomeFilterBuilder = new StringBuilder();
        for(String term : searchTerms){
            if(term.startsWith("@")){
                namespaceFilter = term.toLowerCase().substring(1);
            }else{
                biomeFilterBuilder.append(" ").append(term);
            }
        }
        String biomeFilter = biomeFilterBuilder.isEmpty() ? "" : biomeFilterBuilder.substring(1).toLowerCase();

        List<Component> newRenderComponents = new ArrayList<>();
        for(String namespace : biomeNamespaces){
            if(namespaceFilter != null && !namespace.toLowerCase().contains(namespaceFilter)) continue;
            List<Component> biomeComps = new ArrayList<>();
            for(String biome : namespaceSplitBiomes.get(namespace)){
                if(biome.toLowerCase().contains(biomeFilter)) {
                    biomeComps.add(Component.literal(namespace + ":" + biome));
                }
            }
            if(!biomeComps.isEmpty()) {
                newRenderComponents.add(Component.literal(namespace + ":").withStyle(ChatFormatting.BOLD));
                newRenderComponents.addAll(biomeComps);
            }
        }
        this.renderLines = newRenderComponents;
        this.pageCount = Math.ceilDiv(newRenderComponents.size(), this.renderLinesPerPage);

        if(this.page >= this.pageCount){
            this.page = this.pageCount - 1;
        }
        if(this.pageCount > 0 && this.page < 0) this.page = 0;

        this.previousPageButton.active = this.page > 0;
        this.nextPageButton.active = this.page < this.pageCount - 1;
    }

    @Override
    public void onClose() {
        this.closed = true;
        this.minecraft.setScreenAndShow(this.parent);
    }
}
