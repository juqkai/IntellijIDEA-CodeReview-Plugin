package com.veezean.idea.plugin.codereviewer.model;

import com.veezean.idea.plugin.codereviewer.common.CodeReviewException;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 评审意见信息
 *
 * @author Veezean
 * @since 2022/5/22
 */
public class ReviewComment implements Serializable {

    private static final long serialVersionUID = 90179667808241147L;
    // 服务端交互使用，数据版本，CAS策略控制
    private long dataVersion;
    private int startLine;
    private int endLine;
    private Map<String, ValuePair> propValues = new HashMap<>();

    public long getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(long dataVersion) {
        this.dataVersion = dataVersion;
    }

    public Map<String, ValuePair> getPropValues() {
        return propValues;
    }

    public void setPropValues(Map<String, ValuePair> propValues) {
        this.propValues = propValues;
    }

    public String getStringPropValue(String propName) {
        ValuePair valuePair = propValues.get(propName);
        if (valuePair == null) {
            return null;
        }
        return valuePair.getStringValue();
    }

    public ValuePair getPairPropValue(String propName) {
        return propValues.get(propName);
    }

    public void setStringPropValue(String name, String propValue) {
        propValues.put(name, ValuePair.buildPair(propValue));
    }

    public void setPairPropValue(String name, ValuePair propValue) {
        propValues.put(name, propValue);
    }

    public String getId() {
        return getStringPropValue("identifier");
    }

    public void setId(String id) {
        setStringPropValue("identifier", id);
    }

    public String getFilePath() {
        return getStringPropValue("filePath");
    }

    public void setFilePath(String filePath) {
        setStringPropValue("filePath", filePath);
    }

    public String getComment() {
        return getStringPropValue("comment");
    }

    public void setComment(String comment) {
        setStringPropValue("comment", comment);
    }

    public String getContent() {
        return getStringPropValue("content");
    }

    public void setContent(String content) {
        setStringPropValue("content", content);
    }

    public String getCommitDate() {
        return getStringPropValue("reviewDate");
    }

    public void setCommitDate(String commitDate) {
        setStringPropValue("reviewDate", commitDate);
    }

    public void setRealConfirmer(ValuePair confirmer) {
        setPairPropValue("realConfirmer", confirmer);
    }

    public void setReviewer(ValuePair confirmer) {
        setPairPropValue("reviewer", confirmer);
    }

    public void setConfirmDate(String confirmDate) {
        setStringPropValue("confirmDate", confirmDate);
    }

    public void setConfirmResult(ValuePair confirmResult) {
        setPairPropValue("confirmResult", confirmResult);
    }

    public boolean lineMatched(int currentLine) {
        if (startLine > currentLine || endLine < currentLine) {
            // 范围没有交集
            return false;
        }
        return true;
    }

    public String getLineRange() {
        return getStringPropValue("lineRange");
    }

    public void setLineRange(int startLine, int endLine) {
        int start = startLine + 1;
        int end = endLine + 1;
        String lineRange = start + " ~ " + end;
        setStringPropValue("lineRange", lineRange);

        this.startLine = start;
        this.endLine = end;
    }

    public void setLineRangeInfo() {
        String lineRange = getStringPropValue("lineRange");
        if (StringUtils.isNotEmpty(lineRange)) {
            String[] lines = lineRange.split("~");
            if (lines.length == 2) {
                try {
                    this.startLine = Integer.parseInt(lines[0].trim());
                    this.endLine = Integer.parseInt(lines[1].trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }
}
