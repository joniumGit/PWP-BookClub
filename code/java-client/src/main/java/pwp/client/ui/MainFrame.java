package pwp.client.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.jonium.mason.MasonControl;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;
import pwp.client.Main;
import pwp.client.http.Client;
import pwp.client.model.Book;
import pwp.client.model.Club;
import pwp.client.model.User;
import pwp.client.path.Relations;
import pwp.client.utils.Triple;
import pwp.client.utils.Tuple;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

public class MainFrame extends JFrame {

    @Getter
    private final JPanel base;

    private MainFrame(JPanel basePanel) throws HeadlessException {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(basePanel);
        base = basePanel;
        makeGui();
    }

    private void makeGui() {
        var client = Client.getInstance();
        client.getSM(client.getBase(), new TypeReference<>() {}).thenAcceptAsync(mason -> {
            Main.getLogger().fine("Fetching base");
            base.setLayout(new MigLayout("fill, ins 5", "[60%][40%]"));
            if (mason.isPresent()) {
                var ep = new EntityPanel();
                base.add(ep, "grow, push");
                var tabs = new JTabbedPane();
                var controls = mason.get().getControls();
                if (controls.isEmpty()) {
                    throw new CompletionException(new NullPointerException("Empty Controls"));
                } else {
                    Function<Triple<MasonControl, EntityPanel, Class<?>>, ListPanel<?>> func =
                            triple -> new ListPanel<>(triple.getA(), triple.getB(), triple.getC());
                    for (var tuple : List.of(
                            Tuple.of(Relations.BC_USERS, User.class),
                            Tuple.of(Relations.BC_CLUBS, Club.class),
                            Tuple.of(Relations.BC_BOOKS, Book.class)
                    )
                    ) {
                        var key = tuple.getA();
                        if (controls.containsKey(key.getValue())) {
                            tabs.addTab(
                                    key.getDisplay(),
                                    func.apply(Triple.of(controls.get(key.getValue()), ep, tuple.getB()))
                            );
                        }
                    }
                }

                base.add(tabs, "grow, push");
            } else {
                throw new CompletionException(new NullPointerException("No mason in main"));
            }
        }, SwingUtilities::invokeLater).handleAsync((v, t) -> {
            if (t != null) {
                base.removeAll();
                var label = new JLabel("Failed to fetch data");
                base.add(label, "center, center, push");
                Main.handleException(t);
            }
            this.revalidate();
            return v;
        }, SwingUtilities::invokeLater);
    }

    public static void run(List<String> args) {
        Client.getInstance(args);
        var bp = new JPanel();
        bp.setPreferredSize(new Dimension(640, 480));
        var frame = new MainFrame(bp);
        frame.pack();
        frame.setVisible(true);
    }

}
