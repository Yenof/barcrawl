package com.barcrawl;

import net.runelite.client.ui.PluginPanel;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class BarcrawlPanel extends PluginPanel {
    private final JLabel progress = new JLabel("0/0 (0.00%)", SwingConstants.CENTER);
    private final JCheckBox completedBox = new JCheckBox("Completed", true);
    private final JCheckBox incompleteBox = new JCheckBox("Incomplete", true);
    private final JCheckBox drinkBox = new JCheckBox("Drink", true);
    private final JCheckBox foodBox = new JCheckBox("Food", true);



    private final JPanel listPanel = new JPanel();
    private int completedCount = 0;
    private int totalCount = 0;

    private Set<Integer> allConsumables;
    private Set<Integer> consumedItems;
    private Map<Integer, String> itemNames;
    private Set<Integer> foodItems;
    private Set<Integer> drinkItems;

    public BarcrawlPanel() {
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());

        progress.setFont(progress.getFont().deriveFont(Font.BOLD, 16f));
        progress.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
        header.add(progress, BorderLayout.NORTH);

        JPanel filters = new JPanel(new GridLayout(2, 2));
        filters.add(completedBox);
        filters.add(incompleteBox);
        filters.add(drinkBox);
        filters.add(foodBox);


        header.add(filters, BorderLayout.CENTER);

        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        add(header, BorderLayout.NORTH);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(listPanel);

        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        add (scrollPane, BorderLayout.CENTER);

        incompleteBox.addActionListener(e -> refreshList());
        completedBox.addActionListener(e -> refreshList());
        foodBox.addActionListener(e -> refreshList());
        drinkBox.addActionListener(e -> refreshList());
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension parentSize = getParent() != null ? getParent().getSize() : super.getPreferredSize();

        int width = parentSize.width > 0 ? parentSize.width : 250;
        int height = parentSize.height > 0 ? parentSize.height : 500;

        return new Dimension(width, height);
    }

    public void updateList(
            Set<Integer> allConsumables,
            Set<Integer> consumedItems,
            Map<Integer, String> itemNames,
            Set<Integer> foodItems,
            Set<Integer> drinkItems)
    {
        this.allConsumables = allConsumables;
        this.consumedItems = consumedItems;
        this.itemNames = itemNames;
        this.foodItems = foodItems;
        this.drinkItems = drinkItems;

        SwingUtilities.invokeLater(this::refreshList);
    }
    private static final Set<Integer> EXCLUDED_CONSUMABLES = Set.of(
            739,    // Bravery potion
            756,    // Cadava potion
            11205,  // Shrunk ogleroot
            28383,  // Strange potion
            28388,  // Strangler serum
            25812,  // Sulphur potion
            25813,  // Shielding potion
            33802,  // Mysterious jerky
            29950,  // Principum red
            29969,   // Xochipaltic rosé
            29784, // Araxyte venom sac
            24774, // Blood pint
            22430, // Bloody bracer
            26920, // Big bucket of camel milk
            31856, // Bottle of crystal clear water
            33128, 33130, // Milk sample
            2251, 2257, 2261, 2263, 2265,
            2267, 2269, 2271, 2273, 2275, 2279, // Unfinished battas
            2179, 2181, 2183, 2189, 2193, // Unfinished bowls
            2042, 2044, 2046, 2050, 2052, 2056,// Unfinished cocktails
            2058, 2060, 2062, 2066, 2068, 2070,
            2072, 2076, 2078, 2082, 2086, 2088,
            2090,
            2207, 2211, 2215 // Unfinished crunchy
    );

    private void refreshList() {

        if (allConsumables == null || consumedItems == null || itemNames == null) {
            return;
        }
        Set<String> consumedNames = new HashSet<>();

        for (int id : consumedItems) {
            String name = itemNames.get(id);
            if (name != null) {
                consumedNames.add(name);
            }
        }
        listPanel.removeAll();

        List<Integer> sortedItems = new ArrayList<>(allConsumables);
        sortedItems.sort(Comparator.comparing(id -> itemNames.getOrDefault(id, "")));

        Set<String> displayedNames = new HashSet<>();

        int completed = 0;
        int total = 0;

        for (int itemId : sortedItems) {
            String itemName = itemNames.get(itemId);

            if (itemName == null) {
                continue;
            }

            if (EXCLUDED_CONSUMABLES.contains(itemId)) {
                continue;
            }

            if (!displayedNames.add(itemName)) {
                continue;
            }

            total++;

            boolean unlocked = consumedNames.contains(itemName);

            boolean isFood = foodItems != null && foodItems.contains(itemId);
            boolean isDrink = drinkItems != null && drinkItems.contains(itemId);

            if (unlocked) {
                completed++;
            }


            if (unlocked && !completedBox.isSelected()) {
                continue;
            }
            if (!unlocked && !incompleteBox.isSelected()) {
                continue;
            }
            if (isFood && !foodBox.isSelected()) {
                continue;
            }
            if (isDrink && !drinkBox.isSelected()) {
                continue;
            }

            JLabel label = new JLabel((unlocked ? "✓ " : "✗ ") + itemName);

            label.setForeground(unlocked ? new Color(0, 200, 0) : Color.LIGHT_GRAY);
            label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(label);

        }

        completedCount = completed;
        totalCount = total;

        double percentage = total == 0 ? 0 : (completed * 100.00 / total);
        progress.setText(completed + "/" + total + " (" + String.format("%.2f", percentage) + "%)");
        if (completed == total) {
            progress.setForeground(new Color(0, 200, 0));
            progress.setText(
                    "<html><div style='text-align: center;'>" + completed + "/" + total + " (" + String.format("%.2f", percentage) + "%)" + "<br>" + "You completed Barcrawl Extreme! Good job! \uD83D\uDE04" + "</div></html>"
            );
        }

        listPanel.revalidate();
        listPanel.repaint();
    }
    public int getCompletedCount() {
        return completedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

}
