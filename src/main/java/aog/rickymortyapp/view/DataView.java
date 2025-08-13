package aog.rickymortyapp.view;

import aog.rickymortyapp.model.Charac;
import aog.rickymortyapp.model.Episode;
import aog.rickymortyapp.viewModel.RickAndMortyViewModel;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class DataView extends JFrame {
    private final RickAndMortyViewModel viewModel;
    private JComboBox<String> seasonComboBox;
    private JList<Episode> episodeList;
    private JList<Charac> characterList;
    private JLabel statusLabel;
    private DefaultListModel<Episode> episodeListModel;
    private DefaultListModel<Charac> characterListModel;

    public DataView(RickAndMortyViewModel viewModel) {
        this.viewModel = viewModel;
        initComponents();
        loadSeasons();
    }

    private void initComponents() {
        setTitle("Rick and Morty - Datos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        episodeListModel = new DefaultListModel<>();
        characterListModel = new DefaultListModel<>();
        characterList = new JList<>(characterListModel);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
                20, 20, 20, 20));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel seasonLabel = new JLabel("Temporada:");
        seasonComboBox = new JComboBox<>();
        topPanel.add(seasonLabel);
        topPanel.add(seasonComboBox);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        JPanel episodePanel = new JPanel(new BorderLayout());
        episodePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Episodios", 
            TitledBorder.LEFT, TitledBorder.TOP));
        
        episodeList = new JList<>(episodeListModel);
        episodeList.setCellRenderer(new EpisodeListCellRenderer());
        JScrollPane episodeScrollPane = new JScrollPane(episodeList);
        episodePanel.add(episodeScrollPane, BorderLayout.CENTER);
        
        JPanel characterPanel = new JPanel(new BorderLayout());
        characterPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Personajes", 
            TitledBorder.LEFT, TitledBorder.TOP));
        
        characterList = new JList<>(characterListModel);
        characterList.setCellRenderer(new CharacterListCellRenderer());
        JScrollPane characterScrollPane = new JScrollPane(characterList);
        characterPanel.add(characterScrollPane, BorderLayout.CENTER);
        
        splitPane.setLeftComponent(episodePanel);
        splitPane.setRightComponent(characterPanel);
        
        statusLabel = new JLabel(
                "Cargando datos...", JLabel.LEFT);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        setupListeners();
        
        add(mainPanel);
    }
    
    private void setupListeners() {
        seasonComboBox.addActionListener(e -> {
            String selectedSeason = (String) seasonComboBox.getSelectedItem();
            
            if (selectedSeason != null) {
                loadEpisodesForSeason(selectedSeason);
            }
        });
        
        episodeList.addListSelectionListener(e -> {
            
            if (!e.getValueIsAdjusting()) {
                Episode selectedEpisode = episodeList.getSelectedValue();
                
                if (selectedEpisode != null) {
                    loadCharactersForEpisode(selectedEpisode);
                }
            }
        });
        viewModel.setOnStatusUpdate(status -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText(status);
            });
        });
    }
    
    private void loadSeasons() {
        Map<String, List<Episode>> seasons = viewModel.getSeasonEpisodes();
        
        if (seasons != null && !seasons.isEmpty()) {
            seasons.keySet().stream()
                .sorted()
                .forEach(seasonComboBox::addItem);

            if (seasonComboBox.getItemCount() > 0) {
                seasonComboBox.setSelectedIndex(0);
            }
        }
    }
    
    private void loadEpisodesForSeason(String season) {
        episodeListModel.clear();
        Map<String, List<Episode>> seasons = viewModel.getSeasonEpisodes();
        List<Episode> episodes = seasons.get(season);
        
        if (episodes != null) {
            episodes.forEach(episodeListModel::addElement);
            statusLabel.setText("Cargados " + 
                    episodes.size() + 
                    " episodios de la temporada " + 
                    season);
        }
    }
    
    private void loadCharactersForEpisode(Episode episode) {
        characterListModel.clear();
        statusLabel.setText("Cargando personajes para " + 
                episode.name() + "...");

        new Thread(() -> {
            List<Charac> characters = viewModel.getEpisodeCharacters(
                    episode.id());
            
            SwingUtilities.invokeLater(() -> {
                characters.forEach(characterListModel::addElement);
                statusLabel.setText("Cargados " + 
                        characters.size() + 
                        " personajes para " + 
                        episode.name());
            });
        }).start();
    }

    
    static class EpisodeListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, 
                boolean cellHasFocus) {
            
            super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Episode episode) {
                setText(episode.episode() +
                        " - " + 
                        episode.name() + 
                        " (" + 
                        episode.air_date() + 
                        ")");
            }
            
            return this;
        }
    }
    
    
    static class CharacterListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, 
                boolean cellHasFocus) {
            
            super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Charac character) {
                setText(character.name() + 
                        " - " + 
                        character.species() + 
                        " (" + 
                        character.status() + 
                        ")");
            }    
            return this;
        }
    }
}
