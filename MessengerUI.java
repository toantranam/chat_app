import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * MessengerUI — Pure Swing mock UI for a Telegram-like app.
 * - Left sidebar: profile header, search, recent conversations
 * - Center: chat header, message transcript with bubbles, composer with attach/send
 * - Top-right actions: voice call, video call, info
 *
 * This is UI-only (no networking). Wire your sockets/WebRTC later.
 */
public class MessengerUI extends JFrame {
    private final DefaultListModel<Conversation> conversationsModel = new DefaultListModel<>();
    private final JList<Conversation> conversationsList = new JList<>(conversationsModel);

    private final JPanel transcriptPanel = new JPanel();
    private final JScrollPane transcriptScroll = new JScrollPane(transcriptPanel);

    private final JTextField inputField = new JTextField();
    private final JButton attachBtn = new JButton("+");
    private final JButton sendBtn = new JButton("Send");

    private final JButton callBtn = new JButton("Voice");
    private final JButton videoBtn = new JButton("Video");
    private final JButton infoBtn = new JButton("Info");

    private final JLabel chatTitle = new JLabel("Select a chat");
    private final JLabel chatStatus = new JLabel("–");

    public MessengerUI() {
        super("Mini Messenger — Swing UI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);

        setLookAndFeel();
        buildLayout();
        seedDummyData();
        bindEvents();
    }

    private void setLookAndFeel() {
        try { for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
        }} catch (Exception ignored) {}
    }

    private void buildLayout() {
        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setDividerSize(6);
        outerSplit.setResizeWeight(0.28);
        outerSplit.setContinuousLayout(true);
        add(outerSplit, BorderLayout.CENTER);

        // LEFT: Sidebar
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setBorder(new EmptyBorder(10, 10, 10, 6));
        outerSplit.setLeftComponent(left);

        left.add(buildSidebarHeader(), BorderLayout.NORTH);
        left.add(buildConversationList(), BorderLayout.CENTER);

        // RIGHT: Chat area
        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setBorder(new EmptyBorder(10, 6, 10, 10));
        outerSplit.setRightComponent(right);

        right.add(buildChatHeader(), BorderLayout.NORTH);
        right.add(buildTranscript(), BorderLayout.CENTER);
        right.add(buildComposer(), BorderLayout.SOUTH);
    }

    private JComponent buildSidebarHeader() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Profile row
        JPanel profileRow = new JPanel(new BorderLayout());
        profileRow.setBorder(new EmptyBorder(6, 6, 6, 6));
        JLabel avatar = circleLabel("T");
        JLabel me = new JLabel("Toan");
        me.setFont(me.getFont().deriveFont(Font.BOLD, 16f));
        JButton newChat = new JButton("New Chat");
        profileRow.add(avatar, BorderLayout.WEST);
        profileRow.add(me, BorderLayout.CENTER);
        profileRow.add(newChat, BorderLayout.EAST);

        // Search box
        JPanel searchRow = new JPanel(new BorderLayout());
        JTextField search = new JTextField();
        search.putClientProperty("JTextField.placeholderText", "Search");
        searchRow.add(search, BorderLayout.CENTER);
        searchRow.setBorder(new EmptyBorder(0, 6, 6, 6));

        p.add(profileRow);
        p.add(searchRow);
        return p;
    }

    private JComponent buildConversationList() {
        conversationsList.setCellRenderer(new ConversationRenderer());
        conversationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(conversationsList);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    private JComponent buildChatHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel title = new JPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        chatTitle.setFont(chatTitle.getFont().deriveFont(Font.BOLD, 16f));
        chatStatus.setFont(chatStatus.getFont().deriveFont(Font.PLAIN, 12f));
        chatStatus.setForeground(new Color(100,100,100));
        title.add(chatTitle);
        title.add(chatStatus);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(callBtn); actions.add(videoBtn); actions.add(infoBtn);

        header.add(title, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        header.setBackground(new Color(245, 247, 250));
        header.setOpaque(true);
        return header;
    }

    private JComponent buildTranscript() {
        transcriptPanel.setLayout(new BoxLayout(transcriptPanel, BoxLayout.Y_AXIS));
        transcriptPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        transcriptScroll.setBorder(BorderFactory.createEmptyBorder());
        transcriptScroll.getVerticalScrollBar().setUnitIncrement(18);
        return transcriptScroll;
    }

    private JComponent buildComposer() {
        JPanel bar = new JPanel(new BorderLayout(8, 8));
        bar.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton imageBtn = new JButton("Image");
        JButton fileBtn = new JButton("File");
        JButton videoBtn2 = new JButton("Video");
        left.add(attachBtn); left.add(imageBtn); left.add(fileBtn); left.add(videoBtn2);

        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210,210,210)),
                new EmptyBorder(6,10,6,10)));

        bar.add(left, BorderLayout.WEST);
        bar.add(inputField, BorderLayout.CENTER);
        bar.add(sendBtn, BorderLayout.EAST);

        // Simple handlers for attach buttons (choose file only; no upload)
        ActionListener chooseFile = e -> {
            JFileChooser fc = new JFileChooser();
            int res = fc.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                FileDialogPreview.show(fc.getSelectedFile().getName(), this);
            }
        };
        imageBtn.addActionListener(chooseFile);
        fileBtn.addActionListener(chooseFile);
        videoBtn2.addActionListener(chooseFile);

        return bar;
    }

    private void seedDummyData() {
        conversationsModel.addElement(new Conversation("alice", "Let’s meet at 7?", timeAgo(2)));
        conversationsModel.addElement(new Conversation("bob", "Ok bro", timeAgo(5)));
        conversationsModel.addElement(new Conversation("charlie", "Video ready.", timeAgo(15)));
        conversationsModel.addElement(new Conversation("daisy", "❤️", timeAgo(30)));
        conversationsList.setSelectedIndex(0);
        loadDummyTranscript();
    }

    private String timeAgo(int minutes) { return minutes + "m"; }

    private void loadDummyTranscript() {
        transcriptPanel.removeAll();
        addBubble(new Message("alice", "Hey, are you free tonight?", false));
        addBubble(new Message("me", "Yes. 7pm works.", true));
        addBubble(new Message("alice", "Great! Also check this image.", false));
        addBubble(new Message("alice", "[image: dinner.png]", false));
        addBubble(new Message("me", "Looks nice!", true));
        revalidateTranscript();
    }

    private void bindEvents() {
        conversationsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Conversation c = conversationsList.getSelectedValue();
                if (c != null) {
                    chatTitle.setText(c.name);
                    chatStatus.setText("online");
                    loadDummyTranscript();
                }
            }
        });

        sendBtn.addActionListener(e -> sendCurrent());
        inputField.addActionListener(e -> sendCurrent());

        callBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Placing voice call to " + chatTitle.getText() + "... (mock)"));
        videoBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Starting video call with " + chatTitle.getText() + "... (mock)"));
        infoBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Chat info for " + chatTitle.getText() + "\nMessages: demo transcript"));

        transcriptScroll.getVerticalScrollBar().addAdjustmentListener(e -> {
            // Auto-stick to bottom when new messages arrive if we are near bottom
            if (!e.getValueIsAdjusting()) {
                // noop; placeholder for future
            }
        });
    }

    private void sendCurrent() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        addBubble(new Message("me", text, true));
        inputField.setText("");
        revalidateTranscript();
    }

    private void addBubble(Message m) {
        MessageBubble bubble = new MessageBubble(m);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        if (m.isMine) {
            row.add(bubble, BorderLayout.EAST);
        } else {
            row.add(bubble, BorderLayout.WEST);
        }
        row.setBorder(new EmptyBorder(4, 4, 4, 4));
        transcriptPanel.add(row);
    }

    private void revalidateTranscript() {
        transcriptPanel.revalidate();
        transcriptPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = transcriptScroll.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }

    private JLabel circleLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(210, 230, 255));
                g2.fillOval(0,0,getWidth(),getHeight());
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        l.setOpaque(false);
        l.setPreferredSize(new Dimension(36,36));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
        return l;
    }

    // ===== Models & Renderers =====
    static class Conversation {
        final String name; final String last; final String when;
        Conversation(String name, String last, String when) { this.name = name; this.last = last; this.when = when; }
        @Override public String toString() { return name; }
    }

    static class ConversationRenderer extends JPanel implements ListCellRenderer<Conversation> {
        private final JLabel name = new JLabel();
        private final JLabel last = new JLabel();
        private final JLabel when = new JLabel();
        public ConversationRenderer() {
            setLayout(new BorderLayout(6,2));
            JPanel top = new JPanel(new BorderLayout());
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            when.setForeground(new Color(120,120,120));
            top.add(name, BorderLayout.WEST);
            top.add(when, BorderLayout.EAST);
            last.setForeground(new Color(100,100,100));
            add(top, BorderLayout.NORTH);
            add(last, BorderLayout.SOUTH);
            setBorder(new EmptyBorder(8,8,8,8));
        }
        @Override public Component getListCellRendererComponent(JList<? extends Conversation> list, Conversation value, int index, boolean isSelected, boolean cellHasFocus) {
            name.setText(value.name);
            last.setText(value.last);
            when.setText(value.when);
            setBackground(isSelected ? new Color(230,238,251) : Color.WHITE);
            setOpaque(true);
            return this;
        }
    }

    static class Message {
        final String from; final String text; final boolean isMine; final LocalDateTime ts = LocalDateTime.now();
        Message(String from, String text, boolean isMine) { this.from = from; this.text = text; this.isMine = isMine; }
    }

    static class MessageBubble extends JPanel {
        private final Message m;
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        public MessageBubble(Message m) {
            this.m = m;
            setLayout(new BorderLayout());
            setOpaque(false);
            JLabel text = new JLabel(formatHtml(m.text));
            text.setBorder(new EmptyBorder(8,12,4,12));
            add(text, BorderLayout.CENTER);
            JLabel meta = new JLabel(fmt.format(m.ts));
            meta.setBorder(new EmptyBorder(0, 12, 6, 12));
            meta.setForeground(new Color(80,80,80));
            meta.setFont(meta.getFont().deriveFont(10f));
            add(meta, BorderLayout.SOUTH);
        }
        private String formatHtml(String raw) {
            String esc = raw.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
            return "<html><body style='width: 260px;'>" + esc + "</body></html>";
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 16; int pad = 2;
            int w = getWidth(); int h = getHeight();
            if (m.isMine) {
                g2.setColor(new Color(206, 232, 255));
                g2.fillRoundRect(pad, pad, w - pad*2, h - pad*2, arc, arc);
            } else {
                g2.setColor(new Color(240, 240, 240));
                g2.fillRoundRect(pad, pad, w - pad*2, h - pad*2, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width = Math.min(d.width, 320);
            return d;
        }
    }

    // ===== Simple file preview dialog placeholder =====
    static class FileDialogPreview {
        static void show(String name, Component parent) {
            JOptionPane.showMessageDialog(parent, "Selected: " + name + "\n(Upload stub — integrate HTTP later)");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MessengerUI().setVisible(true));
    }
}
