package com.tcordeiro.moveis.anim;

/**
 * Pás do ventilador: aceleram até à velocidade pedida (sinal de redstone / energia) e, ao desligar, travam em {@link #BRAKE_TICKS}
 * ticks a parar num múltiplo de 90° (as 4 pás ficam onde o modelo estático as desenha: não há salto quando o renderer deixa de
 * desenhar). Ângulo em graus, interpolado com o partialTick. Sem alocações.
 */
public final class Spin {
	/** Graus por tick à velocidade máxima (sinal 15 ou energia): 1 volta por segundo e pouco. */
	public static final float MAX_DEG_PER_TICK = 24f;
	/** Quanto a velocidade (0–1) muda por tick a acelerar. */
	public static final float ACCEL = 0.05f;
	public static final int BRAKE_TICKS = 20;

	private float speed, angle, prevAngle, brakeFrom, brakeTo;
	private int brakeT = -1;

	/** Um tick: {@code target} = velocidade pedida (0 = desligado, 1 = máxima). */
	public void tick(float target) {
		prevAngle = angle;
		if (target > 0) {
			brakeT = -1;
			speed = speed < target ? Math.min(target, speed + ACCEL) : Math.max(target, speed - ACCEL);
			angle += speed * MAX_DEG_PER_TICK;
		} else if (speed > 0 || brakeT >= 0) {
			if (brakeT < 0) { // começa a travar: com desaceleração constante anda v·T/2; acaba no múltiplo de 90° seguinte
				float d = speed * MAX_DEG_PER_TICK * BRAKE_TICKS / 2f;
				brakeFrom = angle;
				brakeTo = (float) Math.ceil((angle + d) / 90.0) * 90f;
				brakeT = 0;
				speed = 0;
			}
			brakeT++;
			float u = Math.min(1f, brakeT / (float) BRAKE_TICKS);
			angle = brakeFrom + (brakeTo - brakeFrom) * (1 - (1 - u) * (1 - u));
			if (brakeT >= BRAKE_TICKS) {
				angle = brakeTo;
				brakeT = -1;
			}
		}
		if (angle >= 360f) { // o mesmo desvio nos dois, para a interpolação não dar a volta para trás
			angle -= 360f;
			prevAngle -= 360f;
			brakeFrom -= 360f;
			brakeTo -= 360f;
		}
	}

	public float angle(float partialTick) {
		return prevAngle + (angle - prevAngle) * partialTick;
	}

	public float speed() {
		return speed;
	}

	/** Parado (e já travado): o modelo estático serve. */
	public boolean stopped() {
		return speed == 0 && brakeT < 0 && prevAngle == angle;
	}
}
