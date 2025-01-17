package com.veezean.idea.plugin.codereviewer.action;

import com.alibaba.fastjson.TypeReference;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.search.PsiShortNamesCache;
import com.veezean.idea.plugin.codereviewer.common.GlobalConfigManager;
import com.veezean.idea.plugin.codereviewer.common.InnerProjectCache;
import com.veezean.idea.plugin.codereviewer.common.NetworkOperationHelper;
import com.veezean.idea.plugin.codereviewer.consts.Constants;
import com.veezean.idea.plugin.codereviewer.consts.InputTypeDefine;
import com.veezean.idea.plugin.codereviewer.model.*;
import com.veezean.idea.plugin.codereviewer.service.ProjectLevelService;
import com.veezean.idea.plugin.codereviewer.util.CommonUtil;
import com.veezean.idea.plugin.codereviewer.util.ExcelResultProcessor;
import com.veezean.idea.plugin.codereviewer.util.LanguageUtil;
import com.veezean.idea.plugin.codereviewer.util.Logger;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 管理评审内容的主界面
 *
 * @author Veezean
 * @since 2019/9/29
 */
public class ManageReviewCommentUI {
    private JButton clearButton;
    private JButton deleteButton;
    private JButton exportButton;
    private JButton importButton;
    private JTable commentTable;
    public JPanel fullPanel;
    private JButton networkConfigButton;
    private JButton updateFromServerButton;
    private JButton commitToServerButton;
    private JComboBox<ServerProjectShortInfo> selectProjectComboBox;
    private JComboBox updateFilterTypecomboBox;
    private JPanel networkButtonGroupPanel;
    private JLabel versionNotes;
    private JLabel showHelpDocButton;
    //    private JLabel serverNoticeLabel2;
    private JButton syncServerCfgDataButton;
    //    private JTextArea serverNoticeArea;
    private JLabel selectProjectLable;
    private JLabel selectTypeLabel;
    private JLabel usageHintLabel;
    private JScrollPane commentMainPanel;
    private JLabel noticeHintLabel;
    private final Project project;

//    private int currentShowMsgIndex = 0;
//    private List<NoticeBody> cachedNotices = new ArrayList<>();
//    private final Object noticeLock = new Object();

//    private JBColor[] NOTICE_COLORS = new JBColor[]{
//            JBColor.BLUE,
//            JBColor.RED,
//            JBColor.GREEN
//    };

    // 记录上一次按住alt点击的时间戳
    private long lastAltClickedTime = -1L;

    public ManageReviewCommentUI(Project project) {
        this.project = project;
        showHelpDocButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                NetworkOperationHelper.openBrowser("https://blog.codingcoder.cn/post/codereviewhelperdoc.html");
            }
        });
        showHelpDocButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void initUI() {
        bindButtons();
        reloadTableData();
        bindTableListeners();
        renderActions();

//        timelyPullFromServer();
        changeLanguageEvent();
    }

//    private void timelyPullFromServer() {
//        ProjectLevelService projectLevelService = ProjectLevelService.getService(ManageReviewCommentUI.this.project);
//        projectLevelService.createScheduler("0 0/5 * * * ?", () -> {
//            if (GlobalConfigManager.getInstance().getGlobalConfig().isNetworkMode()) {
//                NetworkOperationHelper.doGet("client/system/getSystemNotice",
//                        new TypeReference<Response<List<NoticeBody>>>() {
//                        },
//                        notices -> {
//                            synchronized (noticeLock) {
////                                currentShowMsgIndex = 0;
//                                cachedNotices.clear();
//                                cachedNotices.addAll(Optional.ofNullable(notices).map(Response::getData).orElse(new ArrayList<>()));
//                                Logger.info("通知信息拉取更新完成，当前通知数：" + cachedNotices.size());
//
//                            }
//                        }
//                );
//            }
//        });
//
//        // 每5s切换一次通知内容（如果有的话）
//        projectLevelService.createScheduler("0/5 * * * * ?", (Task) () -> {
//            if (GlobalConfigManager.getInstance().getGlobalConfig().isNetworkMode()) {
//                synchronized (noticeLock) {
//                    if (!cachedNotices.isEmpty()) {
//                        currentShowMsgIndex++;
//                        noticeHintLabel.setText(MessageFormat.format(LanguageUtil.getString(
//                                "MAIN_NOTICE_REMINDER"), cachedNotices.size()));
//                        JBColor noticeColor = NOTICE_COLORS[currentShowMsgIndex % NOTICE_COLORS.length];
//                        noticeHintLabel.setForeground(noticeColor);
//                        noticeHintLabel.setBorder(BorderFactory.createLineBorder(noticeColor));
//                        noticeHintLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//                        noticeHintLabel.setVisible(true);
//                    } else {
//                        noticeHintLabel.setVisible(false);
//                    }
//                    if (currentShowMsgIndex > 1000) {
//                        currentShowMsgIndex = 0;
//                    }
//                }
//            }
//        });
//
//        // 每1小时气泡提示一次
//        projectLevelService.createScheduler("0 0 0/1 * * ?", (Task) () -> {
//            if (GlobalConfigManager.getInstance().getGlobalConfig().isNetworkMode()) {
//                String noticeContent = "";
//                synchronized (noticeLock) {
//                    noticeContent = cachedNotices.stream()
//                            .map(NoticeBody::getMsg)
//                            .filter(StringUtils::isNotEmpty)
//                            .map(s -> "· " + s)
//                            .collect(Collectors.joining("<br>"));
//                }
//                if (StringUtils.isNotEmpty(noticeContent)) {
//                    NotificationGroup notificationGroup = new NotificationGroup("CodeReviewNotification",
//                            NotificationDisplayType.TOOL_WINDOW, true);
//                    Notification notification = notificationGroup.createNotification("CodeReview通知提醒", ""
//                            , noticeContent,
//                            NotificationType.WARNING);
//                    Notifications.Bus.notify(notification, ManageReviewCommentUI.this.project);
//                    Logger.info("插件通知新消息弹出，通知内容：" + noticeContent);
//                }
//            }
//        });
//    }

    public void refreshTableDataShow() {
        reloadTableData();
    }

    private void reloadTableData() {
        InnerProjectCache projectCache =
                ProjectLevelService.getService(ManageReviewCommentUI.this.project).getProjectCache();

        RecordColumns recordColumns = GlobalConfigManager.getInstance().getCustomConfigColumns();
        List<Column> availableColumns = recordColumns.getTableAvailableColumns();
        List<Object[]> rowDataList = new ArrayList<>();

        projectCache.getCachedComments()
                .forEach(reviewComment -> {
                    Object[] row = new Object[availableColumns.size()];
                    for (int i = 0; i < availableColumns.size(); i++) {
                        Column column = availableColumns.get(i);
                        if (InputTypeDefine.isComboBox(column.getInputType())) {
                            row[i] = reviewComment.getPairPropValue(column.getColumnCode());
                        } else {
                            row[i] = reviewComment.getStringPropValue(column.getColumnCode());
                        }
                    }
                    rowDataList.add(row);
                });

        Object[][] rowData = rowDataList.toArray(new Object[0][]);

        // 根据配置渲染界面表格
        TableModel dataModel = new CommentTableModel(rowData, recordColumns);
        commentTable.setModel(dataModel);
        commentTable.setEnabled(true);
        for (int i = 0; i < availableColumns.size(); i++) {
            Column column = availableColumns.get(i);
            if (InputTypeDefine.isComboBox(column.getInputType())) {
                JComboBox<ValuePair> comboBox = new ComboBox<>();
                column.getEnumValues().forEach(comboBox::addItem);
                commentTable.getColumnModel().getColumn(i).setCellEditor(new DefaultCellEditor(comboBox));
            }
        }

        commentTable.getModel().addTableModelListener(e -> {
            Logger.info("监听到了表格内容变更事件...");
            int row = e.getFirstRow();

            ReviewComment comment = new ReviewComment();
            for (int i = 0; i < availableColumns.size(); i++) {
                Column column = availableColumns.get(i);
                if (InputTypeDefine.isComboBox(column.getInputType())) {
                    comment.setPairPropValue(column.getColumnCode(), (ValuePair) commentTable.getValueAt(row, i));
                } else {
                    comment.setStringPropValue(column.getColumnCode(), (String) commentTable.getValueAt(row, i));
                }
            }

            ProjectLevelService.getService(ManageReviewCommentUI.this.project).getProjectCache().updateCommonColumnContent(comment);
        });
    }

    private void bindTableListeners() {
        // 指定可编辑列颜色变更
        commentTable.setDefaultRenderer(Object.class, new CommentTableCellRender());

        // 双击跳转到源码位置
        commentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = ((JTable) e.getSource()).rowAtPoint(e.getPoint());
                    int column = ((JTable) e.getSource()).columnAtPoint(e.getPoint());
                    if (!commentTable.isCellEditable(row, column)) {
                        doubleClickDumpToOriginal(ManageReviewCommentUI.this.project, row);
                        return;
                    }
                }
                // 其它场景，默认的处理方法
                super.mouseClicked(e);
            }
        });

        // 按住alt单击，弹出详情确认框
        commentTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // 默认处理点击事件，不能丢
                super.mouseReleased(e);

                // 判断是否摁下alt、且单击场景才响应此事件
                boolean altDown = e.isAltDown();
                int clickCount = e.getClickCount();
                if (!altDown || clickCount > 1) {
                    return;
                }

                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - lastAltClickedTime < 500L) {
                    Logger.info("点击过快，忽略");
                    return;
                } else {
                    lastAltClickedTime = currentTimeMillis;
                }

                int rowAtPoint = commentTable.rowAtPoint(e.getPoint());
                Logger.info("按住alt并点击了表格第" + rowAtPoint + "行");

                // 弹出显示框
                ReviewComment commentInfoModel = ProjectLevelService.getService(ManageReviewCommentUI.this.project)
                        .getProjectCache().getCachedComments().get(rowAtPoint);

                Logger.info("详情确认窗口已经弹出");
                ReviewCommentDialog.show(commentInfoModel, project, Constants.CONFIRM_COMMENT);
                Logger.info("详情确认窗口已经关闭");
            }
        });
    }

    private void doubleClickDumpToOriginal(Project project, int row) {
        String id = (String) commentTable.getValueAt(row, 0);
        ReviewComment commentInfoModel = ProjectLevelService.getService(ManageReviewCommentUI.this.project)
                .getProjectCache().getCachedCommentById(id);

        String filePath = commentInfoModel.getFilePath();
        String packageName = "";
        try {
            String[] splitFilePath = filePath.split("\\,");
            if (splitFilePath.length > 1) {
                packageName = splitFilePath[0];
                filePath = splitFilePath[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
            Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                    LanguageUtil.getString("ALERT_CONTENT_FAILED")
                            + System.lineSeparator()
                            + e.getMessage(),
                    LanguageUtil.getString("ALERT_TITLE_FAILED"));
            return;
        }

        PsiFile[] filesByName = PsiShortNamesCache.getInstance(project).getFilesByName(filePath);
        if (filesByName.length > 0) {
            String targetFilePkgName = packageName;
            PsiFile psiFile = Stream.of(filesByName).filter(psi -> {
                if (psi instanceof PsiJavaFile && StringUtils.isNotEmpty(targetFilePkgName)) {
                    PsiJavaFile javaFile = (PsiJavaFile) psi;
                    String pkgName = javaFile.getPackageName();
                    return StringUtils.equals(pkgName, targetFilePkgName);
                } else {
                    return true;
                }
            }).findFirst().orElse(null);

            if (psiFile == null) {
                Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                        LanguageUtil.getString("ALERT_CONTENT_FAILED") + System.lineSeparator() +
                                LanguageUtil.getString("ALERT_ERR_FILE_NOT_EXIST") + packageName + "." + filePath,
                        LanguageUtil.getString("ALERT_TITLE_FAILED"));
                return;
            }

            VirtualFile virtualFile = psiFile.getVirtualFile();
            // 打开对应的文件
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile);
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
            if (editor == null) {
                Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                        LanguageUtil.getString("ALERT_CONTENT_FAILED") + System.lineSeparator() +
                                LanguageUtil.getString("ALERT_EDITOR_NOT_FOUND"),
                        LanguageUtil.getString(
                                "ALERT_TITLE_FAILED"));
                return;
            }

            // 跳转到指定的位置
            CaretModel caretModel = editor.getCaretModel();
            LogicalPosition logicalPosition = caretModel.getLogicalPosition();
            logicalPosition.leanForward(true);
            LogicalPosition logical = new LogicalPosition(commentInfoModel.getStartLine(), logicalPosition.column);
            caretModel.moveToLogicalPosition(logical);
            SelectionModel selectionModel = editor.getSelectionModel();
            selectionModel.selectLineAtCaret();
        } else {
            Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                    LanguageUtil.getString("ALERT_CONTENT_FAILED") + System.lineSeparator() + LanguageUtil.getString(
                            "ALERT_ERR_FILE_NOT_IN_PROJECT"),
                    LanguageUtil.getString("ALERT_TITLE_FAILED"));
        }

    }

    private void bindButtons() {
        GlobalConfigInfo globalConfig = GlobalConfigManager.getInstance().getGlobalConfig();

        clearButton.addActionListener(e -> {
            int resp = JOptionPane.showConfirmDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                    LanguageUtil.getString("ALERT_CONFIRM_CONTENT"),
                    LanguageUtil.getString("ALERT_TITLE_CONFIRM"),
                    JOptionPane.YES_NO_OPTION);
            if (resp != 0) {
                Logger.info("取消清空操作...");
                return;
            }
            int clearComments =
                    ProjectLevelService.getService(ManageReviewCommentUI.this.project).getProjectCache().clearComments();
            Logger.info("执行清空操作，清空条数： " + clearComments);
            reloadTableData();
        });

        importButton.addActionListener(e -> {

            List<ReviewComment> reviewCommentInfoModels;
            try {
                String recentSelectedFileDir = GlobalConfigManager.getInstance().getRecentSelectedFileDir();
                JFileChooser fileChooser = new JFileChooser(recentSelectedFileDir);
                int saveDialog = fileChooser.showOpenDialog(fullPanel.getRootPane());
                if (saveDialog == JFileChooser.APPROVE_OPTION) {
                    String importPath = fileChooser.getSelectedFile().getPath();

                    GlobalConfigManager.getInstance().saveRecentSelectedFileDir(new File(importPath).getParentFile().getAbsolutePath());

                    reviewCommentInfoModels = ExcelResultProcessor.importExcel(importPath);
                    ProjectLevelService.getService(ManageReviewCommentUI.this.project).getProjectCache()
                            .importComments(reviewCommentInfoModels);
                    CommonUtil.reloadCommentListShow(ManageReviewCommentUI.this.project);
                    Messages.showMessageDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                            LanguageUtil.getString("ALERT_CONTENT_SUCCESS"),
                            LanguageUtil.getString("ALERT_TITLE_SUCCESS"),
                            CommonUtil.getDefaultIcon());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                        LanguageUtil.getString("ALERT_CONTENT_FAILED") + System.lineSeparator() + ex.getMessage(),
                        LanguageUtil.getString(
                                "ALERT_TITLE_FAILED"));
            }
        });

        exportButton.addActionListener(e -> {
            String recentSelectedFileDir = GlobalConfigManager.getInstance().getRecentSelectedFileDir();
            JFileChooser fileChooser = new JFileChooser(recentSelectedFileDir);
            fileChooser.setSelectedFile(new File(LanguageUtil.getString("FILE_NAME_PREFIX") + CommonUtil.getFormattedTimeForFileName()));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel表格(*.xlsx)", ".xlsx"));
            int saveDialog = fileChooser.showSaveDialog(fullPanel.getRootPane());
            if (saveDialog == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getPath();
                if (!path.toLowerCase().endsWith(".xlsx")) {
                    path += ".xlsx";
                }

                String absoluteParentPath = new File(path).getParentFile().getAbsolutePath();
                GlobalConfigManager.getInstance().saveRecentSelectedFileDir(absoluteParentPath);

                try {
                    ExcelResultProcessor.export(path,
                            ProjectLevelService.getService(ManageReviewCommentUI.this.project)
                                    .getProjectCache()
                                    .getCachedComments());
                    Messages.showMessageDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                            LanguageUtil.getString("ALERT_CONTENT_SUCCESS"),
                            LanguageUtil.getString("ALERT_TITLE_SUCCESS"),
                            CommonUtil.getDefaultIcon());
                    Desktop.getDesktop().open(new File(absoluteParentPath));
                } catch (Exception ex) {
                    Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                            LanguageUtil.getString("ALERT_CONTENT_FAILED") + System.lineSeparator() + ex.getMessage(),
                            LanguageUtil.getString("ALERT_TITLE_FAILED"));
                }
            }
        });

        deleteButton.addActionListener(e -> {
            int resp = JOptionPane.showConfirmDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                    LanguageUtil.getString("ALERT_CONFIRM_CONTENT"), LanguageUtil.getString("ALERT_TITLE_CONFIRM"),
                    JOptionPane.YES_NO_OPTION);
            if (resp != 0) {
                Logger.info("取消删除评审记录操作");
                return;
            }

            List<String> deleteIndentifierList = new ArrayList<>();
            int[] selectedRows = commentTable.getSelectedRows();
            if (selectedRows != null && selectedRows.length > 0) {
                for (int rowId : selectedRows) {
                    String valueAt = (String) commentTable.getValueAt(rowId, 0);
                    deleteIndentifierList.add(valueAt);
                }
                ProjectLevelService.getService(ManageReviewCommentUI.this.project).getProjectCache().deleteComments(deleteIndentifierList);
            }

            reloadTableData();
        });

        // 网络版本相关逻辑
        networkConfigButton.addActionListener(e -> NetworkConfigUI.showDialog(ManageReviewCommentUI.this.fullPanel.getRootPane()));

        syncServerCfgDataButton.addActionListener(e -> {
            try {
                pullColumnConfigsFromServer();
                switchNetButtonStatus();

                // 拉取项目列表
                if (GlobalConfigManager.getInstance().getGlobalConfig().isNetworkMode()) {
                    NetworkOperationHelper.doGet("client/project/getMyProjects",
                            new TypeReference<Response<List<ServerProjectShortInfo>>>() {
                            },
                            serverProjectShortInfos -> {
                                // 列表数据缓存到本地
                                globalConfig.setCachedProjectList(serverProjectShortInfos.getData());
                                GlobalConfigManager.getInstance().saveGlobalConfig();
                                resetProjectSelectBox(serverProjectShortInfos.getData());
                            });
                }

                Messages.showMessageDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(), LanguageUtil.getString(
                        "ALERT_CONTENT_SUCCESS"),
                        LanguageUtil.getString("ALERT_TITLE_SUCCESS"),
                        CommonUtil.getDefaultIcon());
            } catch (Exception ex) {
                Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                        LanguageUtil.getString("ALERT_CFG_SYNC_FAILED") + System.lineSeparator() + ex.getMessage(),
                        LanguageUtil.getString("ALERT_CFG_SYNC_FAILED"));
            }
        });

        // 本地缓存的项目信息先初始化出来
        Optional.ofNullable(globalConfig.getCachedProjectList()).ifPresent(this::resetProjectSelectBox);

        // 切换项目的时候的监听方法
        selectProjectComboBox.addItemListener(e -> {
            ServerProjectShortInfo selectedItem = (ServerProjectShortInfo) selectProjectComboBox.getSelectedItem();
            Optional.ofNullable(selectedItem).ifPresent(serverProjectShortInfo -> {
                // 缓存当前选中的记录
                globalConfig.setSelectedServerProjectId(selectedItem.getProjectId());
                GlobalConfigManager.getInstance().saveGlobalConfig();
            });
        });

        // 提交本地内容到服务端
        commitToServerButton.addActionListener(e -> {
            CommitComment commitComment = buildCommitCommentData();
            int resp = JOptionPane.showConfirmDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                    MessageFormat.format(LanguageUtil.getString("MAIN_ALERT_BEFORE_COMMIT"),
                            commitComment.getComments().size()),
                    LanguageUtil.getString("ALERT_TITLE_CONFIRM"),
                    JOptionPane.YES_NO_OPTION);
            if (resp != 0) {
                Logger.info("取消提交操作");
                return;
            }

            // 子线程操作防止界面卡死
            AtomicBoolean isSuccess = new AtomicBoolean(true);
            StringBuffer errInfo = new StringBuffer("");
            Thread workThread = new Thread(() -> {
                try {
                    commitToServerButton.setEnabled(false);
                    NetworkOperationHelper.doPost("client/comment/commitComments",
                            commitComment,
                            new TypeReference<Response<CommitResult>>() {
                            },
                            respBody -> {
                                CommitResult commitResult = respBody.getData();
                                if (!commitResult.isSuccess()) {
                                    errInfo.append(commitResult.getErrDesc())
                                            .append(System.lineSeparator());
                                    if (commitResult.getFailedIds() != null) {
                                        errInfo.append(
                                                commitResult.getFailedIds().stream().collect(Collectors.joining(",",
                                                        "[", "]"))
                                        );
                                    }
                                    isSuccess.set(false);
                                }
                                Map<String, Long> versionMap = commitResult.getVersionMap();
                                if (versionMap != null) {
                                    List<ReviewComment> cachedComments =
                                            ProjectLevelService.getService(ManageReviewCommentUI.this.project)
                                                    .getProjectCache()
                                                    .getCachedComments();
                                    cachedComments.forEach(reviewComment -> {
                                        Long version = versionMap.get(reviewComment.getId());
                                        if (version != null) {
                                            reviewComment.setDataVersion(version);
                                        }
                                    });

                                    // 写入本地，并刷新表格显示
                                    ProjectLevelService.getService(ManageReviewCommentUI.this.project).getProjectCache()
                                            .importComments(cachedComments);
                                    CommonUtil.reloadCommentListShow(ManageReviewCommentUI.this.project);

                                }
                            }
                    );
                } catch (Exception ex) {
                    Logger.error("上传评审数据失败", ex);
                    isSuccess.set(false);
                } finally {
                    commitToServerButton.setEnabled(true);
                }
            });
            workThread.start();

            try {
                workThread.join();
            } catch (Exception ex) {
                Logger.error("上传评审数据失败", ex);
            }

            if (isSuccess.get()) {
                Messages.showMessageDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                        LanguageUtil.getString("ALERT_CONTENT_SUCCESS"),
                        LanguageUtil.getString("ALERT_TITLE_SUCCESS"),
                        CommonUtil.getDefaultIcon());
            } else {
                Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                        MessageFormat.format(LanguageUtil.getString("COMMIT_DATA_FAILED"), errInfo.toString()),
                        LanguageUtil.getString("ALERT_TITLE_FAILED"));
            }
        });

        // 从服务端拉取内容到本地
        updateFromServerButton.addActionListener(e -> {
            ServerProjectShortInfo selectedProject = (ServerProjectShortInfo) selectProjectComboBox.getSelectedItem();
            if (selectedProject == null) {
                Logger.info("未选中项目");
                Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(), LanguageUtil.getString(
                        "MAIN_ALERT_SELECT_PROJECT_FIRST"),
                        LanguageUtil.getString("ALERT_TITLE_FAILED"));
                return;
            }

            String selectedType = (String) updateFilterTypecomboBox.getSelectedItem();

            int resp = JOptionPane.showConfirmDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                    MessageFormat.format(LanguageUtil.getString("ALERT_CONFIRM_BEFORE_PULL_COMMENT"), selectedType),
                    LanguageUtil.getString("ALERT_TITLE_CONFIRM"),
                    JOptionPane.YES_NO_OPTION);
            if (resp != 0) {
                Logger.info("取消更新操作");
                return;
            }

            Long projectKey = selectedProject.getProjectId();

            // 子线程操作，防止界面卡死
            AtomicBoolean isSuccess = new AtomicBoolean(true);
            Thread workThread = new Thread(() -> {
                try {
                    updateFromServerButton.setEnabled(false);
                    ReviewQueryParams queryParams = new ReviewQueryParams();
                    queryParams.setProjectId(projectKey);
                    queryParams.setType(selectedType);
                    NetworkOperationHelper.doPost("client/comment/queryList",
                            queryParams,
                            new TypeReference<Response<CommitComment>>() {
                            },
                            listResponse -> updateLocalData(listResponse.getData().getComments())
                    );
                } catch (Exception ex) {
                    Logger.error("查询评审信息失败", ex);
                    isSuccess.set(false);
                } finally {
                    updateFromServerButton.setEnabled(true);
                }
            });

            workThread.start();

            try {
                workThread.join();
            } catch (Exception ex) {
                Logger.error("查询评审信息失败", ex);
            }

            if (isSuccess.get()) {
                Messages.showMessageDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
                        LanguageUtil.getString("ALERT_CONTENT_SUCCESS"),
                        LanguageUtil.getString("ALERT_TITLE_SUCCESS"),
                        CommonUtil.getDefaultIcon());
            } else {
                Messages.showErrorDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(), LanguageUtil.getString(
                        "ALERT_COMMON_CONTENT_FAILED"),
                        LanguageUtil.getString("ALERT_TITLE_FAILED"));
            }
        });

//        noticeHintLabel.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                Messages.showMessageDialog(ManageReviewCommentUI.this.fullPanel.getRootPane(),
//                        cachedNotices.stream().map(NoticeBody::getMsg).collect(Collectors.joining(System.lineSeparator())),
//                        LanguageUtil.getString("MAIN_NOTICE_CONTENT_TITLE"),
//                        CommonUtil.getDefaultIcon());
//            }
//        });
    }

    void pullColumnConfigsFromServer() {
        if (GlobalConfigManager.getInstance().getGlobalConfig().isNetworkMode()) {
            NetworkOperationHelper.doGet("client/system/pullColumnDefines",
                    new TypeReference<Response<RecordColumns>>() {
                    },
                    recordColumnsResponse -> {
                        Optional.ofNullable(recordColumnsResponse)
                                .map(Response::getData)
                                .ifPresent(recordColumns -> {
                                    // 拉取到服务端配置信息，缓存到本地
                                    GlobalConfigManager.getInstance().saveCustomConfigColumn(recordColumns);
                                });
                    }
            );
        }
    }

    private void resetProjectSelectBox(List<ServerProjectShortInfo> serverProjectShortInfos) {
        selectProjectComboBox.removeAllItems();
        serverProjectShortInfos.forEach(serverProjectShortInfo -> selectProjectComboBox.addItem(serverProjectShortInfo));
        // 如果此前的项目仍存在，则保持选中项目不变
        Optional.ofNullable(GlobalConfigManager.getInstance().getGlobalConfig()
                .getSelectedServerProjectId()).flatMap(projId -> serverProjectShortInfos.stream()
                .filter(serverProjectShortInfo -> serverProjectShortInfo.getProjectId().equals(projId))
                .findFirst()).ifPresent(serverProjectShortInfo -> selectProjectComboBox.setSelectedItem(serverProjectShortInfo));
    }

    private void updateLocalData(List<CommentBody> comments) {
        try {
            if (comments != null) {
                List<ReviewComment> commentInfoModelList = comments.stream()
                        .map(comment -> {
                            ReviewComment reviewComment = new ReviewComment();
                            reviewComment.setDataVersion(comment.getDataVersion());
                            reviewComment.setPropValues(comment.getValues());
                            reviewComment.setLineRangeInfo();
                            return reviewComment;
                        }).collect(Collectors.toList());

                // 写入本地，并刷新表格显示
                ProjectLevelService.getService(ManageReviewCommentUI.this.project).getProjectCache()
                        .importComments(commentInfoModelList);
                CommonUtil.reloadCommentListShow(ManageReviewCommentUI.this.project);
            }
        } catch (Exception e) {
            Logger.error("更新本地表格数据失败", e);
        }
    }

    private CommitComment buildCommitCommentData() {
        List<CommentBody> comments = generateCommitList();
        CommitComment commitComment = new CommitComment();
        commitComment.setComments(comments);
        return commitComment;
    }

    private List<CommentBody> generateCommitList() {
        // 本地内容构造成服务端需要的格式，提交服务端
        List<ReviewComment> cachedComments = ProjectLevelService.getService(ManageReviewCommentUI.this.project)
                .getProjectCache()
                .getCachedComments();
        return cachedComments.stream()
                .map(reviewCommentInfoModel -> {
                    CommentBody comment = new CommentBody();
                    comment.convertAndSetValues(reviewCommentInfoModel.getPropValues());
                    comment.setId(reviewCommentInfoModel.getId());
                    comment.setDataVersion(reviewCommentInfoModel.getDataVersion());
                    return comment;
                }).collect(Collectors.toList());
    }

    private void renderActions() {
        switchNetButtonStatus();
    }

    /**
     * 根据配置是否网络版本，切换相关按钮是否可用
     */
    void switchNetButtonStatus() {
        if (GlobalConfigManager.getInstance().getGlobalConfig().isNetworkMode()) {
            networkButtonGroupPanel.setVisible(true);
            versionNotes.setText(LanguageUtil.getString("MAIN_HINT_SERVER_MODE"));
            // 本地缓存的项目信息先初始化出来
            Optional.ofNullable(GlobalConfigManager.getInstance().getGlobalConfig().getCachedProjectList()).ifPresent(this::resetProjectSelectBox);
            // 显示通知信息区域
            noticeHintLabel.setVisible(true);
        } else {
            networkButtonGroupPanel.setVisible(false);
            versionNotes.setText(LanguageUtil.getString("MAIN_HINT_LOCAL"));
            // 去掉通知信息区域
            noticeHintLabel.setVisible(false);
        }

        // 重新根据配置情况刷新下表格内容
        CommonUtil.reloadCommentListShow(ManageReviewCommentUI.this.project);
    }

    void changeLanguageEvent() {
        Optional.ofNullable(commentMainPanel.getBorder())
                .filter(border -> border instanceof TitledBorder)
                .map(border -> (TitledBorder) border)
                .ifPresent(titledBorder -> {
                    titledBorder.setTitle(LanguageUtil.getString("MAIN_COMMENT_LIST_TITLE"));
                });

        networkConfigButton.setText(LanguageUtil.getString("MAIN_SETTING_BUTTON"));
        syncServerCfgDataButton.setText(LanguageUtil.getString("MAIN_SYNC_CONFIG_BUTTON"));
        selectProjectLable.setText(LanguageUtil.getString("MAIN_SELECT_PROJECT_LABEL"));
        commitToServerButton.setText(LanguageUtil.getString("MAIN_PUSH_TO_SERVER_BUTTON"));
        selectTypeLabel.setText(LanguageUtil.getString("MAIN_SELECT_TYPE_LABEL"));
        updateFromServerButton.setText(LanguageUtil.getString("MAIN_PULL_FROM_SERVER_BUTTON"));
        deleteButton.setText(LanguageUtil.getString("MAIN_DELETE_SELECTED_BUTTON"));
        importButton.setText(LanguageUtil.getString("MAIN_IMPORT_BUTTON"));
        exportButton.setText(LanguageUtil.getString("MAIN_EXPORT_BUTTON"));
        clearButton.setText(LanguageUtil.getString("MAIN_CLEAR_ALL_BUTTON"));
        usageHintLabel.setText(LanguageUtil.getString("MAIN_USAGE_HINT"));
        showHelpDocButton.setText(LanguageUtil.getString("MAIN_USAGE_DOC"));
    }
}
