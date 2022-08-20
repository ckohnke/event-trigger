package gg.xp.xivsupport.events.nextuiws;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.misc.Compressor;
import gg.xp.xivsupport.persistence.Compressible;
import org.intellij.lang.annotations.Language;

import java.io.Serial;

@SystemEvent
public class NuiWsRawMsg extends BaseEvent implements Compressible {
	@Serial
	private static final long serialVersionUID = -7390177233308577948L;
	private String rawMsgData;
	private byte[] compressed;

	public NuiWsRawMsg(@Language("JSON") String rawMsgData) {
		this.rawMsgData = rawMsgData;
	}

	public String getRawMsgData() {
		String raw = this.rawMsgData;
		if (raw != null) {
			return raw;
		}
		else {
			return Compressor.uncompressBytesToString(compressed);
		}
	}

	@Override
	public void compress() {
		byte[] compressed = Compressor.compressStringToBytes(rawMsgData);
		// Only compress if it actually saves memory
		if (compressed.length < rawMsgData.length()) {
			this.compressed = compressed;
			rawMsgData = null;
		}
	}

	@Override
	public void decompress() {
		String raw = this.rawMsgData;
		if (raw == null) {
			this.rawMsgData = Compressor.uncompressBytesToString(compressed);
		}
	}
}
