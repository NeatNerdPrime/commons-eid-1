/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package test.integ.be.fedict.commons.eid.client.simulation;

import java.util.Random;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class ErrorCapableBeIdCard extends SimulatedBeIDCard {
	protected static final ResponseAPDU WAITWAITWAIT = new ResponseAPDU(
			new byte[]{0x6c, (byte) 0x00});
	protected static final ResponseAPDU WHO_AM_I = new ResponseAPDU(new byte[]{
			(byte) 0x6d, (byte) 0x00});

	private boolean nextTooFast;
	private boolean nextBitError;
	private boolean nextRandomResponse;
	private boolean nextCardException;
	private boolean nextConfused;
	private int delay;
	private Random random;

	public ErrorCapableBeIdCard(String profile) {
		this(profile, System.currentTimeMillis());
	}

	public ErrorCapableBeIdCard(String profile, long seed) {
		super(profile);
		this.random = new Random(seed);
		this.delay = 0;
	}

	public ErrorCapableBeIdCard introduceTooFastError() {
		this.nextTooFast = true;
		return this;
	}

	public ErrorCapableBeIdCard introduceBitError() {
		this.nextBitError = true;
		return this;
	}

	public ErrorCapableBeIdCard introduceRandomResponse() {
		this.nextRandomResponse = true;
		return this;
	}

	public ErrorCapableBeIdCard introduceCardException() {
		this.nextCardException = true;
		return this;
	}

	public ErrorCapableBeIdCard introduceConfusion() {
		this.nextConfused = true;
		return this;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	@Override
	protected ResponseAPDU transmit(CommandAPDU apdu) throws CardException {
		if (nextConfused) {
			nextConfused = false;
			try {
				Thread.sleep(40000);
			} catch (InterruptedException e) {
			}
			return WHO_AM_I;
		}

		if (nextCardException) {
			nextCardException = false;
			throw new CardException("Fake CardException Introduced By "
					+ this.getClass().getName());
		}

		if (nextTooFast) {
			nextTooFast = false;
			return WAITWAITWAIT;
		}

		if (nextRandomResponse) {
			nextRandomResponse = false;
			byte[] randomAPDU = new byte[16];
			random.nextBytes(randomAPDU);
			return new ResponseAPDU(randomAPDU);
		}

		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
			}
		}

		if (nextBitError) {
			ResponseAPDU response = super.transmit(apdu);
			byte[] responseBytes = response.getBytes();

			// flip some bits
			responseBytes[random.nextInt(responseBytes.length)] ^= (byte) random
					.nextInt();

			return new ResponseAPDU(responseBytes);
		}

		return super.transmit(apdu);
	}
}
