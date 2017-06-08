package test.com.downloadlibrary.model;

/**
 * Created by Stas on 07.06.17.
 */

public class SomeObject implements Comparable{

    private String fileUrl = "http://cdndl.zaycev.net/807943/4468107/ESTRADARADA_-_%D0%92%D0%B8%D1%82%D0%B5+%D0%9D%D0%B0%D0%B4%D0%BE+%D0%92%D1%8B%D0%B9%D1%82%D0%B8.mp3";
    private String fileName = "sdcard/barbara.mp3";
    private Integer priority = 0;


    public void incrementPriority() {
        priority++;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof SomeObject) {
            return ((SomeObject) o).priority.compareTo(priority);
        }
        return 0;
    }
}
