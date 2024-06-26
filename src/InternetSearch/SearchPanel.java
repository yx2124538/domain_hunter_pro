package InternetSearch;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.google.gson.Gson;

import GUI.GUIMain;
import burp.BurpExtender;
import burp.IPAddressUtils;
import title.WebIcon;
import utils.DomainNameUtils;
import utils.URLUtils;

public class SearchPanel extends JPanel {

	JLabel lblSummary;

	JTabbedPane centerPanel;
	GUIMain guiMain;
	PrintWriter stdout;
	PrintWriter stderr;

	public static void main(String[] args) {
		test();
	}

	public static void test() {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("test");
			frame.setSize(400, 300);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);


			SearchResultEntry test = new SearchResultEntry();
			test.setHost("8.8.8.8");
			test.setPort(88);
			test.setProtocol("https");

			SearchTableModel searchTableModel= new SearchTableModel(null,new ArrayList<SearchResultEntry>(Collections.singletonList(test)));
			SearchTable searchTable = new SearchTable(null,searchTableModel);

			frame.getContentPane().add(searchTable);
		});
	}

	public static void test1() {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("test");
			SearchPanel spanel = new SearchPanel(null);
			frame.getContentPane().add(spanel);
			frame.setSize(400, 300);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
			SearchResultEntry test = new SearchResultEntry();
			test.setHost("8.8.8.8");
			test.setPort(88);
			test.setProtocol("https");
			spanel.addSearchTab("111",new ArrayList<SearchResultEntry>(Collections.singletonList(test)),new ArrayList<String>(Collections.singletonList("xxx")));
		});
	}

	public SearchPanel(GUIMain guiMain) {
		this.guiMain = guiMain;

		try{
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		}catch (Exception e){
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}

		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));
		this.add(createButtonPanel(), BorderLayout.NORTH);
		centerPanel = new JTabbedPane();

		this.add(centerPanel,BorderLayout.CENTER);
	}

	public void addSearchTab(String tabName,List<SearchResultEntry> entries,List<String> engines) {
		JPanel containerpanel = new JPanel();//Tab的最外层容器面板
		containerpanel.setLayout(new BorderLayout(0, 0));

		SearchTableModel searchTableModel= new SearchTableModel(this.guiMain,entries);
		SearchTable searchTable = new SearchTable(this.guiMain,searchTableModel);
		JScrollPane scrollPane = new JScrollPane(searchTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);//table area

		JLabel status = new JLabel("^_^");
		status.setText(getStatusInfo(entries,engines));

		containerpanel.add(scrollPane,BorderLayout.CENTER);
		containerpanel.add(status,BorderLayout.SOUTH);


		//用一个panel实现tab那个小块
		JPanel tabPanel = new JPanel(new BorderLayout());

		JLabel titleLabel = new JLabel(tabName);
		tabPanel.add(titleLabel, BorderLayout.CENTER);

		JButton closeButton = new JButton("x");
		closeButton.setMargin(new Insets(0, 2, 0, 2)); // 设置按钮边距
		closeButton.setFocusable(false); // 禁用焦点
		closeButton.addActionListener(new CloseTabListener(centerPanel, containerpanel));
		tabPanel.add(closeButton, BorderLayout.EAST);


		centerPanel.addTab(null, containerpanel);
		int index = centerPanel.getTabCount() - 1;
		centerPanel.setTabComponentAt(index, tabPanel);


		centerPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					showPopupMenu(centerPanel,e);
				}
			}
		});
	}

	public String getStatusInfo(List<SearchResultEntry> entries,List<String> engines) {
		Map<String,Integer> status = new HashMap<>();
		for (String engine:engines) {
			status.put(engine, 0);
		}
		int unknown=0;
		for (SearchResultEntry entry:entries){
			String source = entry.getSource();
			if (engines.contains(source)) {
				int num = status.get(source);
				status.put(source, num + 1);
			}else {
				unknown++;
			}
		}
		if (unknown>0) {
			status.put("unknown", unknown);
		}

		return new Gson().toJson(status);
	}


	static class CloseTabListener implements ActionListener {
		private JTabbedPane tabbedPane;
		private Component component;

		public CloseTabListener(JTabbedPane tabbedPane, Component component) {
			this.tabbedPane = tabbedPane;
			this.component = component;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			tabbedPane.remove(component);
		}
	}

	// 显示右键菜单
	private void showPopupMenu(JTabbedPane tabbedPane,MouseEvent e) {
		JPopupMenu popupMenu = new JPopupMenu();

		int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
		if (tabIndex == -1) {
			return;
		}
		// 添加菜单项：关闭当前 tab
		JMenuItem closeCurrentTabMenuItem = new JMenuItem("Close Current Tab");
		closeCurrentTabMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tabbedPane.remove(tabIndex);
			}
		});
		popupMenu.add(closeCurrentTabMenuItem);

		// 添加菜单项：关闭所有 tab
		JMenuItem closeAllTabsMenuItem = new JMenuItem("Close All Tabs");
		closeAllTabsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tabbedPane.removeAll();
			}
		});
		popupMenu.add(closeAllTabsMenuItem);

		// 添加菜单项：关闭至左边
		JMenuItem closeTabsToLeftMenuItem = new JMenuItem("Close Tabs to Left");
		closeTabsToLeftMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = tabIndex - 1; i >= 0; i--) {
					tabbedPane.remove(i);
				}
			}
		});
		popupMenu.add(closeTabsToLeftMenuItem);

		// 添加菜单项：关闭至右边
		JMenuItem closeTabsToRightMenuItem = new JMenuItem("Close Tabs to Right");
		closeTabsToRightMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = tabbedPane.getTabCount() - 1; i > tabIndex; i--) {
					tabbedPane.remove(i);
				}
			}
		});
		popupMenu.add(closeTabsToRightMenuItem);

		// 显示右键菜单
		popupMenu.show(tabbedPane, e.getX(), e.getY());
	}

	public JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JTextField textFieldSearch = new JTextField();
		textFieldSearch.setColumns(30);
		buttonPanel.add(textFieldSearch);

		JButton buttonSearch = new JButton("Search");
		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Void, Void> worker = new SwingWorker<Void,Void>() {
					@Override
					protected Void doInBackground() throws Exception {
						String content = textFieldSearch.getText();
						String searchType=null;

						if(DomainNameUtils.isValidDomain(content)) {
							searchType = SearchType.SubDomain;
						}else if (IPAddressUtils.isValidIP(content)) {
							searchType = SearchType.IP;
						}else {
							searchType = SearchType.OriginalString;
						}

						APISearchAction.DoSearchAllInOn(searchType,content,SearchEngine.getAssetSearchEngineList());

						return null;
					}

					@Override
					protected void done() {

					} 
				};
				worker.execute();
			}
		});
		buttonPanel.add(buttonSearch);


		JButton buttonSearchAs = new JButton("Search As");
		buttonSearchAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingWorker<Void, Void> worker = new SwingWorker<Void,Void>() {
					@Override
					protected Void doInBackground() throws Exception {
						String content = textFieldSearch.getText();

						String searchType = SearchType.choseSearchType();
						switch (searchType){
							case SearchType.Email:
								APISearchAction.DoSearchAllInOn(searchType,content,SearchEngine.getEmailSearchEngineList());
								break;
							case SearchType.IconHash:
								if (URLUtils.isVaildUrl(content)){
									byte[] imageData = WebIcon.getFavicon(content);
									if (imageData.length>0){
										content = WebIcon.getHash(imageData);
									}
								}
							default:
								APISearchAction.DoSearchAllInOn(searchType,content,SearchEngine.getAssetSearchEngineList());
						}
						return null;
					}

					@Override
					protected void done() {

					}
				};
				worker.execute();
			}
		});
		buttonPanel.add(buttonSearchAs);


		lblSummary = new JLabel("^_^");
		buttonPanel.add(lblSummary);
		buttonPanel.setToolTipText("");

		return buttonPanel;
	}
}
