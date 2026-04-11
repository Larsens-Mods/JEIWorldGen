package de.larsensmods.jeiworldgen.config;

public class ClientConfig {

    private boolean combineSimilarDatasets = true;
    private boolean showNoDataMessage = true;

    public void setCombineSimilarDatasets(boolean combineSimilarDatasets) {
        this.combineSimilarDatasets = combineSimilarDatasets;
    }

    public void setShowNoDataMessage(boolean showNoDataMessage) {
        this.showNoDataMessage = showNoDataMessage;
    }

    public boolean combineSimilarDatasets(){
        return this.combineSimilarDatasets;
    }

    public boolean showNoDataMessage(){
        return this.showNoDataMessage;
    }

}
