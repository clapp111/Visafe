package com.visafe.domain.notification.service;

import com.visafe.domain.notification.enums.NotificationType;

/**
 * 알림 제목과 내용. 받는 사람의 화면 언어(ko, vi)로 만들며, 그 밖의 언어는 한국어로 보낸다.
 *
 * @param title 제목
 * @param message 내용
 */
public record NotificationMessage(String title, String message) {

    private static final String VIETNAMESE = "vi";

    /**
     * 알림 종류와 언어에 맞는 문구를 만든다.
     *
     * @param type 알림 종류
     * @param language 받는 사람의 화면 언어
     * @param contractNo 관련 계약 번호
     * @return 제목과 내용
     */
    public static NotificationMessage of(NotificationType type, String language, String contractNo) {
        return VIETNAMESE.equals(language) ? vietnamese(type, contractNo) : korean(type, contractNo);
    }

    private static NotificationMessage korean(NotificationType type, String no) {
        return switch (type) {
            case REVIEW_REQUESTED -> new NotificationMessage("확인할 계약이 도착했습니다", "계약 " + no + "의 내용을 확인하고 응답해 주세요.");
            case CONTRACT_RETURNED ->
                new NotificationMessage("계약이 돌아왔습니다", "계약 " + no + "이(가) 돌아왔습니다. 돌아온 사유를 확인하고 내용을 고쳐 다시 제출해 주세요.");
            case TRANSLATION_FAILED ->
                new NotificationMessage("번역에 실패했습니다", "계약 " + no + "의 번역이 3번 모두 실패했습니다. 다시 시도해 주세요.");
            case CONTRACT_CONFIRMED -> new NotificationMessage("계약이 확정되었습니다", "계약 " + no + "이(가) 확정되어 확인번호가 발급되었습니다.");
            case TERMINATION_REQUESTED ->
                new NotificationMessage("계약 종료 요청이 도착했습니다", "계약 " + no + "의 종료 요청을 확인하고 동의 여부를 알려 주세요.");
            case TERMINATION_RESPONDED ->
                new NotificationMessage("종료 요청에 응답이 왔습니다", "계약 " + no + "의 종료 요청에 상대방이 응답했습니다.");
            case CONTRACT_TERMINATED -> new NotificationMessage("계약이 종료되었습니다", "계약 " + no + "이(가) 종료되었습니다.");
            case AUTHORITY_REVIEWED ->
                new NotificationMessage("기관 검토 결과가 나왔습니다", "계약 " + no + "의 감독 기관 검토 결과를 확인해 주세요.");
        };
    }

    private static NotificationMessage vietnamese(NotificationType type, String no) {
        return switch (type) {
            case REVIEW_REQUESTED ->
                new NotificationMessage(
                        "Có hợp đồng cần bạn xác nhận", "Vui lòng xem nội dung hợp đồng " + no + " và phản hồi.");
            case CONTRACT_RETURNED ->
                new NotificationMessage(
                        "Hợp đồng đã được trả lại",
                        "Hợp đồng " + no + " đã được trả lại. Vui lòng xem lý do, sửa nội dung và gửi lại.");
            case TRANSLATION_FAILED ->
                new NotificationMessage(
                        "Dịch thất bại", "Bản dịch hợp đồng " + no + " đã thất bại 3 lần. Vui lòng thử lại.");
            case CONTRACT_CONFIRMED ->
                new NotificationMessage(
                        "Hợp đồng đã được xác nhận", "Hợp đồng " + no + " đã được xác nhận và đã cấp mã xác nhận.");
            case TERMINATION_REQUESTED ->
                new NotificationMessage(
                        "Có yêu cầu chấm dứt hợp đồng",
                        "Vui lòng xem yêu cầu chấm dứt hợp đồng " + no + " và cho biết bạn có đồng ý hay không.");
            case TERMINATION_RESPONDED ->
                new NotificationMessage(
                        "Đã có phản hồi về yêu cầu chấm dứt",
                        "Bên kia đã phản hồi yêu cầu chấm dứt hợp đồng " + no + ".");
            case CONTRACT_TERMINATED ->
                new NotificationMessage("Hợp đồng đã chấm dứt", "Hợp đồng " + no + " đã chấm dứt.");
            case AUTHORITY_REVIEWED ->
                new NotificationMessage(
                        "Đã có kết quả thẩm định",
                        "Vui lòng xem kết quả thẩm định của cơ quan quản lý cho hợp đồng " + no + ".");
        };
    }
}
