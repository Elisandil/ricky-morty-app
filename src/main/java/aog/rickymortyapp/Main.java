package aog.rickymortyapp;

import aog.rickymortyapp.view.StartView;
import aog.rickymortyapp.viewModel.RickAndMortyViewModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | 
                    IllegalAccessException | 
                    InstantiationException | 
                    UnsupportedLookAndFeelException e) {

            }
            RickAndMortyViewModel viewModel = new RickAndMortyViewModel();
            StartView startView = new StartView(viewModel);
            startView.setVisible(true);
        });
    }
}
