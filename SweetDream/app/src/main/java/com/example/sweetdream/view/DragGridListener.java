package com.example.sweetdream.view;

public interface DragGridListener {

    /**
     *
     * @param oldPosition
     * @param newPosition
     */
    public void reorderItems(int oldPosition, int newPosition);


    /**
     *
     * @param hidePosition
     */
    public void setHideItem(int hidePosition);


    /**
     *
     * @param deletePosition
     */
    public void removeItem(int deletePosition);

    /**
     *
     * @param openPosition
     */
    void setItemToFirst(int openPosition);

    void nitifyDataRefresh();

}
