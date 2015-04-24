package at.yawk.httpserver;

/**
 * @author yawkat
 */
interface State {
    byte BEFORE_STATUS = 0;
    byte IN_HEADERS = 1;
    byte IN_BODY = 2;
}
