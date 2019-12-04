package burp;

import protobuf.ProtoBuf;
import java.awt.Component;

public class BurpExtender implements IBurpExtender, IMessageEditorTabFactory {

    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        callbacks.setExtensionName("Protocol Buffers Raw Decoder");
        callbacks.registerMessageEditorTabFactory(this);
    }

    @Override
    public IMessageEditorTab createNewInstance(IMessageEditorController controller, boolean editable) {
        return new ProtoBufTab(controller, false);
    }

    class ProtoBufTab implements IMessageEditorTab {

        private final ITextEditor txtInput;
        private byte[] currentMessage;

        public ProtoBufTab(IMessageEditorController controller, boolean editable) {
            txtInput = callbacks.createTextEditor();
            txtInput.setEditable(editable);
        }

        @Override
        public String getTabCaption() {
            return "ProtoBuf";
        }

        @Override
        public Component getUiComponent() {
            return txtInput.getComponent();
        }

        @Override
        public boolean isEnabled(byte[] content, boolean isRequest) {
            return content != null && content.length > 0;
        }

        private int findBody(byte[] content) {
            for (int i = 0; i < (content.length - 4); i++) {
                if (content[i] == 0xD && content[i + 1] == 0xA && content[i + 2] == 0xD && content[i + 3] == 0xA) {
                    return i + 4;
                }
            }
            return content.length;
        }

        @Override
        public void setMessage(byte[] content, boolean isRequest) {
            if (content == null) {
                txtInput.setText(null);
            } else {
                String s;
                try {
                    int offset = isRequest
                            ? helpers.analyzeRequest(content).getBodyOffset()
                            : helpers.analyzeResponse(content).getBodyOffset();
                    s = ProtoBuf.decode(content, offset);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    s = e.getMessage();
                }
                txtInput.setText(s.getBytes());
            }
            currentMessage = content;
        }

        @Override
        public byte[] getMessage() {
            return currentMessage;
        }

        @Override
        public boolean isModified() {
            return txtInput.isTextModified();
        }

        @Override
        public byte[] getSelectedData() {
            return txtInput.getSelectedText();
        }
    }
}
