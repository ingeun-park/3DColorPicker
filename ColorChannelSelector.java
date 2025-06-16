package view;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.Timer;

import doc.myFont;

/**
 * 3D 회전 휠 스타일 색상 채널
 */
public class ColorChannelSelector extends JComponent {
	private static final long serialVersionUID = 1L;

	private static final String[] CHANNELS = { "RED", "GREEN", "BLUE", "R-G", "R-B", "G-R", "G-B", "B-R", "B-G" };

	// 파스텔 색으로 변경
	private static final Color[] CHANNEL_COLORS = { new Color(255, 182, 193), // RED
			new Color(152, 251, 152), 
			new Color(173, 216, 230), 
			new Color(255, 218, 185),
			new Color(255, 192, 203),
			new Color(211, 255, 166), 
			new Color(175, 238, 238), 
			new Color(221, 160, 221), 
			new Color(176, 224, 230) 
	};

	private static final int COMPONENT_WIDTH = 290;
	private static final int COMPONENT_HEIGHT = 80;
	private static final int ITEM_WIDTH = 70;
	private static final int ITEM_HEIGHT = 50;

	// 오프셋 및 애니메이션 설정
	private static final double MAX_OFFSET = 6.0;
	private static final double VISIBLE_RANGE = 4.5;
	private static final double ANIMATION_SPEED = 0.15;
	private static final double ANIMATION_THRESHOLD = 0.001;
	private static final double OFFSET_RESET_THRESHOLD = 0.1;

	// 드래그 설정
	private static final double DRAG_SENSITIVITY = 1.2;

	// 스케일 및 투명도 설정
	private static final double MIN_SCALE = 0.5;
	private static final double SCALE_FACTOR = 0.25;
	private static final double MIN_ALPHA = 0.2;
	private static final double ALPHA_FACTOR = 0.3;
	private static final double Y_OFFSET_FACTOR = 3.0;

	// 폰트 설정
	private static final int BASE_FONT_SIZE = 15;
	private static final int MIN_FONT_SIZE = 10;

	// 배경 색상 설정
	private static final Color BACKGROUND_COLOR = Color.WHITE;
	private static final Color SELECTION_HIGHLIGHT_COLOR = new Color(216, 191, 216, 100);
	private static final Color TEXT_COLOR = Color.BLACK;
	private static final int UNSELECTED_ALPHA = 150;

	private int selectedIndex = 0;
	private double offset = 0.0; // 현재 스크롤 오프셋
	private boolean isDragging = false;
	private int dragStartX = 0;
	private double dragStartOffset = 0.0;

	// 애니메이션
	private Timer animationTimer;
	private double targetOffset = 0.0;

	// 콜백
	private Consumer<String> onSelectionChanged;

	public ColorChannelSelector() {
		validateConfiguration();
		setPreferredSize(new Dimension(COMPONENT_WIDTH, COMPONENT_HEIGHT));
		setMinimumSize(new Dimension(COMPONENT_WIDTH, COMPONENT_HEIGHT));
		setMaximumSize(new Dimension(COMPONENT_WIDTH, COMPONENT_HEIGHT));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		initializeEventHandlers();
		initializeAnimation();
	}

	/**
	 * 설정의 유효성
	 */
	private void validateConfiguration() {
		if (CHANNELS.length != CHANNEL_COLORS.length) {
			throw new IllegalStateException("채널 배열과 색상 배열의 크기가 일치하지 않습니다. " + "CHANNELS: " + CHANNELS.length
					+ ", CHANNEL_COLORS: " + CHANNEL_COLORS.length);
		}

		if (CHANNELS.length == 0) {
			throw new IllegalStateException("최소 하나 이상의 채널이 필요합니다.");
		}
	}

	/**
	 * 선택 변경 콜백 설정
	 */
	public void setOnSelectionChanged(Consumer<String> callback) {
		this.onSelectionChanged = callback;
	}

	/**
	 * 현재 선택된 채널 반환
	 */
	public String getSelectedChannel() {
		return CHANNELS[selectedIndex];
	}

	/**
	 * 채널 선택
	 */
	public void setSelectedChannel(String channel) {
		for (int i = 0; i < CHANNELS.length; i++) {
			if (CHANNELS[i].equals(channel)) {
				selectedIndex = i;
				resetOffsets();
				repaint();
				break;
			}
		}
	}

	/**
	 * 이벤트 핸들러
	 */
	private void initializeEventHandlers() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				isDragging = true;
				dragStartX = e.getX();
				dragStartOffset = offset;
				stopAnimation();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				isDragging = false;
				snapToNearestItem();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (isDragging) {
					int deltaX = e.getX() - dragStartX;
					double newOffset = dragStartOffset - (deltaX / (double) (ITEM_WIDTH * DRAG_SENSITIVITY));
					setOffset(newOffset);
				}
			}
		});
	}

	/**
	 * 애니메이션 초기화
	 */
	private void initializeAnimation() {
		animationTimer = new Timer(16, e -> { // ~60 FPS
			if (Math.abs(offset - targetOffset) > ANIMATION_THRESHOLD) {
				offset += (targetOffset - offset) * ANIMATION_SPEED;
				repaint();
			} else {
				offset = targetOffset;
				cleanupOffsets();
				stopAnimation();
			}
		});
	}

	/**
	 * 애니메이션 시작
	 */
	private void startAnimation() {
		if (!animationTimer.isRunning()) {
			animationTimer.start();
		}
	}

	/**
	 * 애니메이션 중지
	 */
	private void stopAnimation() {
		if (animationTimer.isRunning()) {
			animationTimer.stop();
		}
	}

	/**
	 * 오프셋 설정
	 */
	private void setOffset(double newOffset) {
		this.offset = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, newOffset));// 범위 제한
		repaint();
	}

	/**
	 * 오프셋 리셋
	 */
	private void resetOffsets() {
		offset = 0.0;
		targetOffset = 0.0;
	}

	/**
	 * 오프셋 정리
	 */
	private void cleanupOffsets() {
		if (Math.abs(offset) < ANIMATION_THRESHOLD) {
			offset = 0.0;
		}
		if (Math.abs(targetOffset) < ANIMATION_THRESHOLD) {
			targetOffset = 0.0;
		}
	}

	/**
	 * selectedIndex 순환 범위 내로 제한
	 */
	private void normalizeSelectedIndex() {
		selectedIndex = ((selectedIndex % CHANNELS.length) + CHANNELS.length) % CHANNELS.length;
	}

	/**
	 * 가장 가까운 아이템으로 스냅
	 */
	private void snapToNearestItem() {
		double roundedOffset = Math.round(offset);

		// 선택된 인덱스 업데이트
		int offsetChange = (int) roundedOffset;
		selectedIndex = selectedIndex - offsetChange;
		normalizeSelectedIndex();

		// 선택 변경 콜백 호출
		if (onSelectionChanged != null) {
			onSelectionChanged.accept(getSelectedChannel());
		}

		// 애니메이션으로 정확한 위치로 이동
		targetOffset = roundedOffset;
		startAnimation();

		// 오프셋 리셋
		if (Math.abs(roundedOffset) > OFFSET_RESET_THRESHOLD) {
			offset -= roundedOffset;
			targetOffset = 0.0;
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g.create();
		try {
			setupRenderingHints(g2d);

			int centerX = getWidth() / 2;
			int centerY = getHeight() / 2;

			drawBackground(g2d, centerX, centerY);
			drawChannelItems(g2d, centerX, centerY);
		} finally {
			g2d.dispose();
		}
	}

	/**
	 * 렌더링 힌트
	 */
	private void setupRenderingHints(Graphics2D g2d) {
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	/**
	 * 배경
	 */
	private void drawBackground(Graphics2D g2d, int centerX, int centerY) {
		// 전체 배경
		g2d.setColor(BACKGROUND_COLOR);
		g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

		// 중앙 선택 영역 표시
		g2d.setColor(SELECTION_HIGHLIGHT_COLOR);
		g2d.fillRoundRect(centerX - ITEM_WIDTH / 2 - 3, centerY - ITEM_HEIGHT / 2 - 3, ITEM_WIDTH + 6, ITEM_HEIGHT + 6,
				8, 8);
	}

	/**
	 * 모든 채널 아이템 그리기
	 */
	private void drawChannelItems(Graphics2D g2d, int centerX, int centerY) {
		// 화면에 보일 수 있는 범위의 아이템만 그림
		int minPosition = (int) Math.floor(-VISIBLE_RANGE - offset);
		int maxPosition = (int) Math.ceil(VISIBLE_RANGE - offset);

		for (int position = minPosition; position <= maxPosition; position++) {
			drawChannelItem(g2d, position, centerX, centerY);
		}
	}

	/**
	 * 개별 채널 아이템 그리기
	 */
	private void drawChannelItem(Graphics2D g2d, int position, int centerX, int centerY) {
		double actualPosition = position + offset;

		// 너무 먼경우
		if (Math.abs(actualPosition) > VISIBLE_RANGE) {
			return;
		}

		// 인덱스 계산
		int channelIndex = calculateChannelIndex(position);

		// 위치 및 효과 계산
		ItemRenderInfo renderInfo = calculateRenderInfo(actualPosition, centerX, centerY, channelIndex);

		// 렌더링
		renderChannelItem(g2d, renderInfo);
	}

	/**
	 * 채널 인덱스 계산
	 */
	private int calculateChannelIndex(int position) {
		int channelIndex = selectedIndex + position;
		return ((channelIndex % CHANNELS.length) + CHANNELS.length) % CHANNELS.length;
	}

	/**
	 * 아이템 렌더링 정보 계산
	 */
	private ItemRenderInfo calculateRenderInfo(double actualPosition, int centerX, int centerY, int channelIndex) {
		// 위치 계산
		double x = centerX + actualPosition * ITEM_WIDTH;

		// 중앙에서의 거리에 따른 스케일과 투명도 계산
		double distance = Math.abs(actualPosition);
		double scale = Math.max(MIN_SCALE, 1.0 - distance * SCALE_FACTOR);
		double alpha = Math.max(MIN_ALPHA, 1.0 - distance * ALPHA_FACTOR);

		// 3D 효과를 위한 Y 오프셋
		double yOffset = distance * Y_OFFSET_FACTOR;
		double y = centerY + yOffset;

		// 크기 계산
		int itemWidth = (int) (ITEM_WIDTH * scale);
		int itemHeight = (int) (ITEM_HEIGHT * scale);
		int itemX = (int) (x - itemWidth / 2);
		int itemY = (int) (y - itemHeight / 2);

		return new ItemRenderInfo(channelIndex, x, y, itemX, itemY, itemWidth, itemHeight, scale, alpha, distance,
				CHANNELS[channelIndex]);
	}

	/**
	 * 채널 아이템 렌더링
	 */
	private void renderChannelItem(Graphics2D g2d, ItemRenderInfo info) {
		// 투명도 설정
		g2d.setComposite(AlphaComposite.SrcOver.derive((float) info.alpha));

		// 배경 그리기
		drawItemBackground(g2d, info);

		// 텍스트 그리기
		drawItemText(g2d, info);

		// 투명도 리셋
		g2d.setComposite(AlphaComposite.SrcOver);
	}

	/**
	 * 아이템 배경 그리기
	 */
	private void drawItemBackground(Graphics2D g2d, ItemRenderInfo info) {
		Color bgColor = CHANNEL_COLORS[info.channelIndex];

		if (info.distance < OFFSET_RESET_THRESHOLD) {
			// 선택된 아이템은 원래 색상
			g2d.setColor(bgColor);
		} else {
			// 선택되지 않은 아이템은 투명하게
			g2d.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), UNSELECTED_ALPHA));
		}

		g2d.fillRoundRect(info.itemX, info.itemY, info.itemWidth, info.itemHeight, 6, 6);
	}

	/**
	 * 아이템 텍스트 그리기
	 */
	private void drawItemText(Graphics2D g2d, ItemRenderInfo info) {
		g2d.setColor(TEXT_COLOR);

		int fontSize = Math.max(MIN_FONT_SIZE, (int) (BASE_FONT_SIZE * info.scale));
		Font font = myFont.font(1, fontSize);
		g2d.setFont(font);

		FontMetrics fm = g2d.getFontMetrics(font);
		int textWidth = fm.stringWidth(info.text);
		int textHeight = fm.getAscent();

		int textX = (int) (info.x - textWidth / 2);
		int textY = (int) (info.y + textHeight / 2);

		g2d.drawString(info.text, textX, textY);
	}

	/**
	 * 아이템 렌더링 정보를 담는 데이터 클래스
	 */
	private static class ItemRenderInfo {
		final int channelIndex;
		final double x, y;
		final int itemX, itemY, itemWidth, itemHeight;
		final double scale, alpha, distance;
		final String text;

		ItemRenderInfo(int channelIndex, double x, double y, int itemX, int itemY, int itemWidth, int itemHeight,
				double scale, double alpha, double distance, String text) {
			this.channelIndex = channelIndex;
			this.x = x;
			this.y = y;
			this.itemX = itemX;
			this.itemY = itemY;
			this.itemWidth = itemWidth;
			this.itemHeight = itemHeight;
			this.scale = scale;
			this.alpha = alpha;
			this.distance = distance;
			this.text = text;
		}
	}
}
