import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class GoogleCSE_SearchApp extends JFrame {

    // ====== Store API key/cx in Preferences (khỏi set ENV) ======
    private static final Preferences PREF = Preferences.userNodeForPackage(GoogleCSE_SearchApp.class);
    private static final String PREF_KEY = "GOOGLE_CSE_KEY";
    private static final String PREF_CX  = "GOOGLE_CSE_CX";

    private String apiKey = PREF.get(PREF_KEY, "");
    private String cx     = PREF.get(PREF_CX, "");

    // ====== HTTP/JSON ======
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    // ====== UI Models ======
    private final DefaultListModel<String> domainModel = new DefaultListModel<>();
    private final JList<String> domainList = new JList<>(domainModel);

    private final DefaultListModel<ResultItem> resultModel = new DefaultListModel<>();
    private final JList<ResultItem> resultList = new JList<>(resultModel);

    private final JTextField tfK1 = new JTextField();
    private final JTextField tfK2 = new JTextField();
    private final JTextField tfK3 = new JTextField();

    private final JButton btnSearch = new JButton("Tìm kiếm");
    private final JButton btnPrice  = new JButton("Tìm giá sản phẩm");
    private final JButton btnSettings = new JButton("⚙");

    private final JLabel lbStatus = new JLabel("Sẵn sàng");
    private final JProgressBar progress = new JProgressBar();

    // Filter
    private List<ResultItem> allResults = new ArrayList<>();
    private String domainFilter = "Tất cả";

    public GoogleCSE_SearchApp() {
        super("Tìm kiếm tin trên internet");

        setupLookAndFeel();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));
        setLocationRelativeTo(null);

        setContentPane(buildUI());

        btnSearch.addActionListener(e -> runSearch(false));
        btnPrice.addActionListener(e -> runSearch(true));
        btnSettings.addActionListener(e -> openSettingsDialog(true));

        domainList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = domainList.getSelectedValue();
                domainFilter = (sel == null) ? "Tất cả" : sel;
                applyFilter();
            }
        });

        // double click open link
        resultList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ResultItem it = resultList.getSelectedValue();
                    if (it != null) openBrowser(it.link);
                }
            }
        });

        // Nếu chưa có key/cx thì bật dialog
        if (apiKey.isBlank() || cx.isBlank()) openSettingsDialog(false);
    }

    private void setupLookAndFeel() {
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 16);
        UIManager.put("Button.arc", 16);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.margin", new Insets(10, 14, 10, 14));
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.setBackground(UIManager.getColor("Panel.background"));

        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildTopBar() {
        JPanel top = new GradientHeader();
        top.setLayout(new BorderLayout(12, 12));
        top.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Tìm kiếm tin trên internet");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel subtitle = new JLabel("Nhập từ khóa → tìm trên Google Custom Search API");
        subtitle.setForeground(new Color(255, 255, 255, 210));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        btnSettings.setFocusPainted(false);
        btnSettings.setOpaque(false);
        btnSettings.setForeground(Color.WHITE);
        btnSettings.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,120), 1, true));
        btnSettings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(btnSettings);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel(new GridLayout(1, 3, 12, 12));
        body.setOpaque(false);

        body.add(buildDomainsCard());
        body.add(buildSearchCard());
        body.add(buildResultsCard());

        return body;
    }

    private JComponent buildDomainsCard() {
        RoundedCard card = new RoundedCard();
        card.setLayout(new BorderLayout(10, 10));

        JLabel lb = sectionTitle("Danh sách web");
        card.add(lb, BorderLayout.NORTH);

        domainList.setModel(domainModel);
        domainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        domainList.setFixedCellHeight(30);
        domainList.setBorder(new EmptyBorder(6, 6, 6, 6));

        domainModel.clear();
        domainModel.addElement("Tất cả");
        domainList.setSelectedIndex(0);

        JScrollPane sp = new JScrollPane(domainList);
        sp.setBorder(BorderFactory.createEmptyBorder());
        card.add(sp, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html><span style='color:#666'>Click domain để lọc kết quả</span></html>");
        card.add(hint, BorderLayout.SOUTH);

        return card;
    }

    private JComponent buildSearchCard() {
        RoundedCard card = new RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(sectionTitle("Từ khóa"));
        card.add(Box.createVerticalStrut(10));

        styleTextField(tfK1, "Từ khóa 1 (bắt buộc)");
        styleTextField(tfK2, "Từ khóa 2 (tuỳ chọn)");
        styleTextField(tfK3, "Từ khóa 3 (tuỳ chọn)");

        card.add(tfK1);
        card.add(Box.createVerticalStrut(10));
        card.add(tfK2);
        card.add(Box.createVerticalStrut(10));
        card.add(tfK3);
        card.add(Box.createVerticalStrut(14));

        btnSearch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPrice.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel btns = new JPanel(new GridLayout(2, 1, 0, 10));
        btns.setOpaque(false);
        btns.add(btnSearch);
        btns.add(btnPrice);

        card.add(btns);
        card.add(Box.createVerticalGlue());

        JLabel tip = new JLabel("<html><span style='color:#666'>Mẹo: bấm ⚙ để nhập API Key/CX</span></html>");
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(tip);

        return card;
    }

    private JComponent buildResultsCard() {
        RoundedCard card = new RoundedCard();
        card.setLayout(new BorderLayout(10, 10));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);

        JLabel lb = sectionTitle("Kết quả");
        JLabel hint = new JLabel("<html><span style='color:#666'>Double-click để mở link</span></html>");
        hint.setHorizontalAlignment(SwingConstants.RIGHT);

        head.add(lb, BorderLayout.WEST);
        head.add(hint, BorderLayout.EAST);

        resultList.setModel(resultModel);
        resultList.setCellRenderer(new ResultRenderer());
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setFixedCellHeight(-1); // allow renderer preferred size
        resultList.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane sp = new JScrollPane(resultList);
        sp.setBorder(BorderFactory.createEmptyBorder());

        card.add(head, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBorder(new EmptyBorder(10, 2, 0, 2));
        bar.setOpaque(false);

        progress.setIndeterminate(false);
        progress.setVisible(false);

        bar.add(lbStatus, BorderLayout.WEST);
        bar.add(progress, BorderLayout.EAST);
        return bar;
    }

    private JLabel sectionTitle(String text) {
        JLabel lb = new JLabel(text);
        lb.setFont(lb.getFont().deriveFont(Font.BOLD, 14f));
        return lb;
    }

    private void styleTextField(JTextField tf, String placeholder) {
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tf.putClientProperty("JTextField.placeholderText", placeholder); // FlatLaf supports
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
    }

    // ====== Settings Dialog ======
    private void openSettingsDialog(boolean allowCancel) {
        JDialog dlg = new JDialog(this, "Cấu hình Google Custom Search API", true);
        dlg.setLayout(new BorderLayout(12, 12));
        dlg.setSize(520, 280);
        dlg.setLocationRelativeTo(this);

        JPanel content = new JPanel();
        content.setBorder(new EmptyBorder(14, 14, 14, 14));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPasswordField tfKey = new JPasswordField(apiKey);
        JTextField tfCx = new JTextField(cx);

        tfKey.putClientProperty("JTextField.placeholderText", "API Key (AIza...)");
        tfCx.putClientProperty("JTextField.placeholderText", "CX (Search engine ID)");

        styleTextField(tfKey, "API Key (AIza...)");
        styleTextField(tfCx, "CX (Search engine ID)");

        JLabel note = new JLabel("<html><span style='color:#666'>Key/CX sẽ lưu trên máy (Preferences). Không nên hard-code.</span></html>");

        content.add(sectionTitle("API Key"));
        content.add(Box.createVerticalStrut(6));
        content.add(tfKey);
        content.add(Box.createVerticalStrut(12));
        content.add(sectionTitle("CX"));
        content.add(Box.createVerticalStrut(6));
        content.add(tfCx);
        content.add(Box.createVerticalStrut(12));
        content.add(note);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = new JButton("Lưu");
        JButton btnCancel = new JButton("Hủy");

        btnSave.addActionListener(e -> {
            String k = new String(tfKey.getPassword()).trim();
            String c = tfCx.getText().trim();
            if (k.isBlank() || c.isBlank()) {
                JOptionPane.showMessageDialog(dlg, "Bạn phải nhập đủ API Key và CX.", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
                return;
            }
            apiKey = k;
            cx = c;
            PREF.put(PREF_KEY, apiKey);
            PREF.put(PREF_CX, cx);
            dlg.dispose();
            toast("Đã lưu cấu hình.");
        });

        btnCancel.addActionListener(e -> {
            if (!allowCancel && (apiKey.isBlank() || cx.isBlank())) {
                JOptionPane.showMessageDialog(dlg, "Bạn cần cấu hình API Key/CX để dùng chức năng tìm kiếm.", "Bắt buộc", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dlg.dispose();
        });

        actions.add(btnCancel);
        actions.add(btnSave);

        dlg.add(content, BorderLayout.CENTER);
        dlg.add(actions, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

private void toast(String msg) {
    lbStatus.setText(msg);
    javax.swing.Timer t = new javax.swing.Timer(3000, e -> lbStatus.setText("Sẵn sàng"));
    t.setRepeats(false);
    t.start();
}


    // ====== Search ======
    private void runSearch(boolean priceMode) {
        if (apiKey.isBlank() || cx.isBlank()) {
            openSettingsDialog(false);
            return;
        }

        List<String> keywords = collectKeywords();
        if (keywords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập ít nhất Từ khóa 1.", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String query = String.join(" ", keywords) + (priceMode ? " giá" : "");

        setBusy(true, "Đang tìm: " + query);

        SwingWorker<List<ResultItem>, Void> worker = new SwingWorker<>() {
            @Override protected List<ResultItem> doInBackground() throws Exception {
                return googleSearch(query, 10);
            }

            @Override protected void done() {
                try {
                    allResults = get();
                    buildDomainsFromResults(allResults);
                    domainFilter = "Tất cả";
                    domainList.setSelectedIndex(0);
                    applyFilter();
                    setBusy(false, "Xong. Tổng: " + allResults.size() + " kết quả");
                } catch (Exception ex) {
                    setBusy(false, "Có lỗi");
                    JOptionPane.showMessageDialog(GoogleCSE_SearchApp.this,
                            "Lỗi: " + ex.getMessage(),
                            "API Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void setBusy(boolean busy, String status) {
        btnSearch.setEnabled(!busy);
        btnPrice.setEnabled(!busy);
        btnSettings.setEnabled(!busy);

        progress.setVisible(busy);
        progress.setIndeterminate(busy);

        lbStatus.setText(status);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void buildDomainsFromResults(List<ResultItem> items) {
        LinkedHashSet<String> domains = new LinkedHashSet<>();
        for (ResultItem it : items) domains.add(it.displayLink);

        domainModel.clear();
        domainModel.addElement("Tất cả");
        for (String d : domains) domainModel.addElement(d);
    }

    private void applyFilter() {
        resultModel.clear();
        for (ResultItem it : allResults) {
            if ("Tất cả".equals(domainFilter) || it.displayLink.equalsIgnoreCase(domainFilter)) {
                resultModel.addElement(it);
            }
        }
        if (resultModel.size() == 0) {
            resultModel.addElement(new ResultItem("Không có kết quả theo bộ lọc", "", "Hãy chọn “Tất cả” hoặc tìm lại.", domainFilter));
        }
    }

    private List<String> collectKeywords() {
        List<String> ks = new ArrayList<>();
        addIfNotBlank(ks, tfK1.getText());
        addIfNotBlank(ks, tfK2.getText());
        addIfNotBlank(ks, tfK3.getText());
        return ks;
    }

    private void addIfNotBlank(List<String> list, String s) {
        if (s == null) return;
        s = s.trim();
        if (!s.isEmpty()) list.add(s);
    }

    private List<ResultItem> googleSearch(String query, int num) throws Exception {
        String q = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String url = "https://www.googleapis.com/customsearch/v1"
                + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&cx="  + URLEncoder.encode(cx, StandardCharsets.UTF_8)
                + "&q=" + q
                + "&num=" + Math.min(Math.max(num, 1), 10)
                + "&hl=vi&gl=vn&safe=active";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(18))
                .header("User-Agent", "Mozilla/5.0 JavaHttpClient")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = mapper.readTree(resp.body());

        JsonNode err = root.path("error");
        if (!err.isMissingNode() && err.has("message")) {
            throw new RuntimeException(err.path("message").asText());
        }
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " — " + resp.body());
        }

        List<ResultItem> out = new ArrayList<>();
        JsonNode items = root.path("items");
        if (items.isArray()) {
            for (JsonNode it : items) {
                String title = it.path("title").asText("");
                String link = it.path("link").asText("");
                String snippet = it.path("snippet").asText("");
                String displayLink = it.path("displayLink").asText(hostOf(link));
                if (!link.isBlank()) out.add(new ResultItem(title, link, snippet, displayLink));
            }
        }
        return out;
    }

    private static String hostOf(String url) {
        try { return URI.create(url).getHost(); }
        catch (Exception e) { return url; }
    }

    private void openBrowser(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {}
    }

    // ====== Result UI ======
    private static class ResultItem {
        final String title, link, snippet, displayLink;
        ResultItem(String title, String link, String snippet, String displayLink) {
            this.title = title == null ? "" : title;
            this.link = link == null ? "" : link;
            this.snippet = snippet == null ? "" : snippet;
            this.displayLink = displayLink == null ? "" : displayLink;
        }
        @Override public String toString() { return title; }
    }

    private class ResultRenderer extends JPanel implements ListCellRenderer<ResultItem> {
        private final JLabel lbTitle = new JLabel();
        private final JLabel lbMeta  = new JLabel();
        private final JTextArea taSnippet = new JTextArea();

        ResultRenderer() {
            setLayout(new BorderLayout(8, 6));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            setOpaque(true);

            lbTitle.setFont(lbTitle.getFont().deriveFont(Font.BOLD, 14f));
            lbMeta.setForeground(new Color(110, 110, 110));

            taSnippet.setLineWrap(true);
            taSnippet.setWrapStyleWord(true);
            taSnippet.setEditable(false);
            taSnippet.setOpaque(false);
            taSnippet.setForeground(new Color(150, 30, 30));

            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setOpaque(false);
            top.add(lbTitle, BorderLayout.CENTER);
            top.add(lbMeta, BorderLayout.EAST);

            add(top, BorderLayout.NORTH);
            add(taSnippet, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ResultItem> list, ResultItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            lbTitle.setText(value.title.isBlank() ? value.link : value.title);
            lbMeta.setText(value.displayLink);
            taSnippet.setText(value.snippet);

            Color bg = isSelected ? new Color(230, 242, 255) : Color.WHITE;
            setBackground(bg);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isSelected ? new Color(120, 180, 255) : new Color(238, 238, 238), 1, true),
                    new EmptyBorder(10, 10, 10, 10)
            ));
            return this;
        }
    }

    // ====== Pretty panels ======
    private static class RoundedCard extends JPanel {
        RoundedCard() {
            setOpaque(false);
            setBorder(new EmptyBorder(14, 14, 14, 14));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 20;
            int w = getWidth();
            int h = getHeight();

            // shadow
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(2, 4, w - 4, h - 6, arc, arc);

            // card
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 4, h - 6, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

 private static class GradientHeader extends JPanel {
    GradientHeader() {
        setOpaque(false); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        Color c1 = new Color(12, 74, 110);
        Color c2 = new Color(2, 132, 199);

        GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, w, h, 22, 22);

        g2.dispose();
    }
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GoogleCSE_SearchApp().setVisible(true));
    }
}
