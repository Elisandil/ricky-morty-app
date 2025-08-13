package aog.rickymortyapp.view;

import aog.rickymortyapp.viewModel.RickAndMortyViewModel;
import javax.swing.*;
import java.awt.*;

public class StartView extends JFrame {
    private final RickAndMortyViewModel viewModel;
    private JButton startButton;
    private JButton infoButton;
    private JLabel statusLabel;

    public StartView(RickAndMortyViewModel viewModel) {
        this.viewModel = viewModel;
        initComponents();
        setupListeners();
    }

    private void initComponents() {
        setTitle("Rick and Morty API Explorer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 20, 20, 20));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(
                FlowLayout.CENTER, 20, 20));
        startButton = new JButton("Start");
        infoButton = new JButton("Info");
        
        buttonPanel.add(startButton);
        buttonPanel.add(infoButton);
        
        statusLabel = new JLabel(
                "Listo para comenzar", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void setupListeners() {
        
        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            viewModel.setOnStatusUpdate(this::updateStatus);
            viewModel.setOnEpisodesLoaded(episodes -> {
                SwingUtilities.invokeLater(() -> {
                    DataView dataView = new DataView(viewModel);
                    dataView.setVisible(true);
                });
            });
            viewModel.setOnLoadingComplete(completed -> {
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                });
            });
            new Thread(viewModel::loadData).start();
        });
        
        infoButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                viewModel.getAppInfo(),
                "Información de la Aplicación",
                JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
        });
    }
}
