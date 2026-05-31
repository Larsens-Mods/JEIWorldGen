package de.larsensmods.jeiworldgen.config;

public class ClientConfig {

    private boolean combineSimilarDatasets = true;
    private boolean showNoDataMessage = true;
    private String[] hiddenBlocks = new String[0];

    public void setCombineSimilarDatasets(boolean combineSimilarDatasets) {
        this.combineSimilarDatasets = combineSimilarDatasets;
    }

    public void setShowNoDataMessage(boolean showNoDataMessage) {
        this.showNoDataMessage = showNoDataMessage;
    }

    public void setHiddenBlocks(String[] hiddenBlocks) {
        this.hiddenBlocks = hiddenBlocks;
    }

    public boolean combineSimilarDatasets(){
        return this.combineSimilarDatasets;
    }

    public boolean showNoDataMessage(){
        return this.showNoDataMessage;
    }

    public String[] hiddenBlocks(){
        return this.hiddenBlocks;
    }

}
