package com.example.vaadinchat;

import com.github.rjeschke.txtmark.Processor;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Route("")
//@Push


public class MainView extends VerticalLayout{
    private final Storage storage;
    private Registration registration;

    private Grid<Storage.ChatMessage> grid;
    private VerticalLayout chat;
    private VerticalLayout login;
    private String user = "";

    public MainView(Storage storage) {
        this.storage = storage;

        buildLogin();
        buildChat();
    }

    private void buildLogin() { // окно для ввода юзернейма
        login = new VerticalLayout() {{ // создание типа вертикального канваса (ну типа! для меня так понятнее)
            TextField field = new TextField();
            field.setPlaceholder("enter your name");
            add( // метод который делает показывает компоненты вертикал лейаут
                    field,
                    new Button("start chat") {{
                        addClickListener(click -> {
                            login.setVisible(false); // окно логинаскрылось
                            chat.setVisible(true);
                            user = field.getValue();
                            storage.addRecordJoined(user);
                        });
                        addClickShortcut(Key.ENTER);
                    }}
            );
        }};
        add(login); // а это зачем?
    }

    private void buildChat() {
        chat = new VerticalLayout();
        add(chat);
        chat.setVisible(false);

        grid = new Grid<>();
        grid.setItems(storage.getMessages()); /// собирает все сообщения из очереди
        grid.addColumn(new ComponentRenderer<>(message -> new Html(renderRow(message))))
                .setAutoWidth(true);

        TextField field = new TextField();

        chat.add(
                new H3("Super Vaadin chat"),
                grid,
                new HorizontalLayout() {{
                    add(
                            field,
                            new Button("➡") {{
                                addClickListener(click -> {
                                    storage.addRecord(user, field.getValue());
                                    field.clear();
                                });
                                addClickShortcut(Key.ENTER);
                            }}
                    );
                }}
        );
    }

    public void onMessage(Storage.ChatEvent event) {
        if (getUI().isPresent()) {
            UI ui = getUI().get();
            ui.getSession().lock();
            ui.beforeClientResponse(grid, ctx -> grid.scrollToEnd());
            ui.access(() -> grid.getDataProvider().refreshAll());
            ui.getSession().unlock();
        }
    }

    private String renderRow(Storage.ChatMessage message) {
        if (message.getName().isEmpty()) {
            return Processor.process(String.format("_User **%s** is just joined the chat!_", message.getMessage()));
        } else {
            return Processor.process(String.format("**%s**: %s", message.getName(), message.getMessage()));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) { //Called after the UI is added to the session.
        registration = storage.attachListener(this::onMessage);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        registration.remove();
    }
}
